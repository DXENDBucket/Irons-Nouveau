package dev.ironsnouveau;
import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import dev.ironsnouveau.bridge.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.ExtendedFireworkRocket;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import net.minecraft.gametest.framework.*;
import net.minecraft.core.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("irons_nouveau_feedback") @PrefixGameTestTemplate(false)
public final class FeedbackGameTests {
    private static LivingEntity cow(GameTestHelper h,int x,int z) {
        var c=h.spawn(EntityType.COW,x,2,z);c.setNoAi(true);c.setNoGravity(true);c.setYRot(-90);c.setXRot(0);
        c.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);c.setHealth(100);return c;
    }
    private static Resolution context(GameTestHelper h,LivingEntity caster,String id) {
        var g=IronsNouveau.glyphs().stream().filter(x->x.definition().spellId().getPath().equals(id)).findFirst().orElseThrow();
        return new Resolution(h.getLevel(),caster,SpellRegistry.getSpell(g.definition().spellId()),g.definition(),new SpellStats.Builder().build());
    }
    @GameTest(template="empty",templateNamespace="irons_nouveau_feedback")
    public static void fireworksExplodeImmediatelyWithNativeDamageAndAttribution(GameTestHelper h) {
        var caster=cow(h,1,1);var victim=cow(h,5,1);
        h.assertTrue(TerrainAdapters.FIRECRACKER.applyAt(context(h,caster,"firecracker"),new EntityHitResult(victim,victim.getBoundingBox().getCenter())),"Firecracker resolves");
        h.assertTrue(victim.getHealth()<100,"Native firework explosion deals damage immediately");
        h.assertTrue(victim.getLastDamageSource()!=null && victim.getLastDamageSource().getEntity()==caster,"Explosion retains caster attribution");
        h.assertTrue(h.getLevel().getEntitiesOfClass(ExtendedFireworkRocket.class,victim.getBoundingBox().inflate(5),e->!e.isRemoved()).isEmpty(),"No inert firework remains waiting for an empty tick method");
        h.succeed();
    }
    @GameTest(template="empty",templateNamespace="irons_nouveau_feedback")
    public static void selfShieldUsesEyeRayAndStopsAtBlocks(GameTestHelper h) {
        var caster=cow(h,1,1);var ctx=context(h,caster,"shield");
        h.assertTrue(PointEntityAdapters.SHIELD.applyAt(ctx,new EntityHitResult(caster)),"Self shield resolves");
        var shield=h.getLevel().getEntitiesOfClass(ShieldEntity.class,caster.getBoundingBox().inflate(5)).get(0);
        h.assertTrue(shield.position().distanceTo(caster.getEyePosition().add(caster.getLookAngle().scale(3)))<.001,"Self shield is ahead at eye height, not at waist/feet");shield.discard();
        var wall=BlockPos.containing(caster.getEyePosition().add(caster.getLookAngle().scale(2)));
        h.getLevel().setBlock(wall,Blocks.STONE.defaultBlockState(),3);
        var expected=io.redspace.ironsspellbooks.api.util.RaycastBuilder.begin(h.getLevel(),caster).range(3).checkForBlocks(true).build().getLocation();
        PointEntityAdapters.SHIELD.applyAt(ctx,new EntityHitResult(caster));
        var clipped=h.getLevel().getEntitiesOfClass(ShieldEntity.class,caster.getBoundingBox().inflate(5),e->!e.isRemoved()).get(0);
        h.assertTrue(clipped.position().distanceTo(expected)<.001 && clipped.position().distanceTo(caster.getEyePosition())<3,"Native wall clipping is preserved");clipped.discard();h.succeed();
    }
    @GameTest(template="empty",templateNamespace="irons_nouveau_feedback")
    public static void explicitShieldPointStaysAtArsHit(GameTestHelper h) {
        var caster=cow(h,1,1);var position=caster.position().add(4,2,1);
        var hit=new BlockHitResult(position,Direction.UP,BlockPos.containing(position),false);
        PointEntityAdapters.SHIELD.applyAt(context(h,caster,"shield"),hit);
        var shield=h.getLevel().getEntitiesOfClass(ShieldEntity.class,new AABB(position,position).inflate(2)).get(0);
        h.assertTrue(shield.position().distanceTo(position)<.001,"Ars remote placement is not redirected to the caster");shield.discard();h.succeed();
    }
}
