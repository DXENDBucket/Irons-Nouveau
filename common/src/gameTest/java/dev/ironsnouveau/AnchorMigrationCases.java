package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.resource.ResourceAccount;
import dev.arsconflux.api.targeting.Allies;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.glyph.BridgeGlyph;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class AnchorMigrationCases {
    private static LivingEntity cow(GameTestHelper h,int x,int z){var e=h.spawn(EntityType.COW,x,3,z);e.setNoAi(true);e.setNoGravity(true);e.setYRot(-90);e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100);e.setHealth(100);return e;}
    private static BridgeGlyph glyph(String id){return IronsNouveau.glyphs().stream().filter(g->g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow();}
    private static SpellContext context(GameTestHelper h,LivingEntity caster,LivingEntity credit,Spell spell,AtomicInteger mana){
        var ars=new SpellContext(h.getLevel(),spell,caster,LivingCaster.from(caster),ItemStack.EMPTY);
        ResourceAccount account=new ResourceAccount(){public Object reservationKey(){return mana;}public boolean canSpend(int amount){return mana.get()>=amount;}public void spend(int amount){mana.addAndGet(-amount);}};
        CastContexts.bind(ars,CastContext.of(ars,null,account).withDamageOwner(credit));return ars;
    }
    private static void cast(GameTestHelper h,LivingEntity caster,LivingEntity credit,String id,HitResult hit,AtomicInteger mana){
        var g=glyph(id);var ars=context(h,caster,credit,new Spell(MethodTouch.INSTANCE,g),mana);
        g.onResolve(hit,h.getLevel(),caster,new SpellStats.Builder().build(),ars,new SpellResolver(ars));
    }
    public static void remoteSiphon(GameTestHelper h){
        var caster=cow(h,1,1);var host=cow(h,3,8);var victim=cow(h,7,8);var credit=cow(h,1,5);var mana=new AtomicInteger(100000);
        var relation=Allies.register(ResourceLocation.tryParse("test:siphon_"+caster.getId()),(a,b)->a==caster&&b==host);
        h.runAtTickTime(2,()->{
            try {
                int cost=SpellRegistry.getSpell(glyph("ray_of_siphoning").definition().spellId()).getManaCost(1);
                cast(h,caster,credit,"ray_of_siphoning",new EntityHitResult(host),mana);
                h.assertTrue(victim.getHealth()<100 && victim.getLastDamageSource().getEntity()==credit,"Remote native Siphon reaches actual ray and keeps separate damage owner");
                h.assertTrue(mana.get()==100000-cost && host.getHealth()==100,"Actual owner pays one pulse, host is excluded");
                h.assertTrue(ExecutionSessions.isOccupied(host,ExecutionClaim.CHANNELING) && !ExecutionSessions.isCasting(caster),"Actual host owns channel, not payer");
                relation.close();ExecutionSessions.tick();
                h.assertTrue(!ExecutionSessions.isCasting(host),"Loss of provider-only alliance releases channel");
                victim.invulnerableTime=0;double before=victim.getHealth();var point=host.getEyePosition();
                cast(h,caster,credit,"ray_of_siphoning",new BlockHitResult(point,Direction.UP,BlockPos.containing(point),false),mana);
                h.assertTrue(victim.getHealth()<before && !ExecutionSessions.isCasting(caster),"Fixed point also hits remote target without occupying caster");h.succeed();
            }catch(Exception e){throw new RuntimeException(e);}finally{caster.discard();host.discard();victim.discard();credit.discard();ExecutionSessions.tick();}
        });
    }
    public static void rayQuery(GameTestHelper h){
        var caster=cow(h,1,1);var victim=cow(h,7,8);Vec3 start=h.absoluteVec(new Vec3(3,3.7,8));var end=start.add(12,0,0);
        var hit=RayGeometry.trace(h.getLevel(),null,start,end,.15f,e->e==victim);
        h.assertTrue(hit instanceof EntityHitResult eh && eh.getEntity()==victim,"Candidates come from the remote ray, not an observer box");
        h.getLevel().setBlock(BlockPos.containing(start.add(2,0,0)),Blocks.STONE.defaultBlockState(),3);
        h.assertTrue(RayGeometry.trace(h.getLevel(),null,start,end,.15f,e->e==victim).getType()==HitResult.Type.BLOCK,"Actual ray respects the wall before a remote entity");
        caster.discard();victim.discard();h.succeed();
    }
    public static void negAndDelay(GameTestHelper h) throws Exception {
        var caster=cow(h,1,1);var host=cow(h,4,5);var mana=new AtomicInteger(100000);
        BeneficialExecutorCases.ally(h,caster,host);
        var self=com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.getSpellPart(ResourceLocation.tryParse("arsomega:glyph_propagate_self"));
        var under=com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.getSpellPart(ResourceLocation.tryParse("arsomega:glyph_propagate_underfoot"));
        h.assertTrue(self!=null&&under!=null,"Real NEG redirect glyphs are present");
        var breath=glyph("poison_breath");
        var ars=context(h,caster,caster,new Spell(MethodTouch.INSTANCE,self,breath),mana);ars.nextPart();
        var resolver=(SpellResolver)Class.forName("alexthw.not_enough_glyphs.api.ThreadwiseSpellResolver").getConstructor(SpellContext.class).newInstance(ars);
        resolver.onResolveEffect(h.getLevel(),new EntityHitResult(host));
        var cone=h.getLevel().getEntitiesOfClass(AbstractConeProjectile.class,caster.getBoundingBox().inflate(40)).get(0);
        h.assertTrue(((ConeState)cone).ironsNouveau$anchor()==caster.getId(),"NEG self uses final entity hit, not original recipient");cone.discard();ExecutionSessions.tick();
        ars=context(h,caster,caster,new Spell(MethodTouch.INSTANCE,under,breath),mana);ars.nextPart();
        resolver=(SpellResolver)Class.forName("alexthw.not_enough_glyphs.api.ThreadwiseSpellResolver").getConstructor(SpellContext.class).newInstance(ars);
        resolver.onResolveEffect(h.getLevel(),new EntityHitResult(host));
        cone=h.getLevel().getEntitiesOfClass(AbstractConeProjectile.class,host.getBoundingBox().inflate(40),e->!e.isRemoved()).get(0);
        h.assertTrue(((ConeState)cone).ironsNouveau$anchor()==-1 && cone.position().distanceTo(host.position())<.001 && !ExecutionSessions.isCasting(caster),"NEG underfoot becomes a fixed point with no entity claim");cone.discard();ExecutionSessions.tick();
        ars=context(h,caster,caster,new Spell(MethodTouch.INSTANCE,com.hollingsworth.arsnouveau.common.spell.effect.EffectDelay.INSTANCE,breath),mana);ars.nextPart();
        new SpellResolver(ars).onResolveEffect(h.getLevel(),new EntityHitResult(host));
        h.runAtTickTime(30,()->{
            var delayed=h.getLevel().getEntitiesOfClass(AbstractConeProjectile.class,host.getBoundingBox().inflate(40),e->!e.isRemoved());
            h.assertTrue(delayed.size()==1 && ((ConeState)delayed.get(0)).ironsNouveau$anchor()==host.getId() && ExecutionSessions.isCasting(host),"Real Ars delay retains attribution/account and resolves actual actor on execution");
            caster.discard();host.discard();ExecutionSessions.tick();h.succeed();
        });
    }
}
