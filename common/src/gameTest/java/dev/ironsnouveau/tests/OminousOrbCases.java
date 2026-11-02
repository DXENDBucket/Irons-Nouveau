package dev.ironsnouveau.tests;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.resource.ResourceAccount;
import dev.arsconflux.api.targeting.Allies;
import dev.ironsnouveau.ability.*;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.fire_orb.OminousFireOrbEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class OminousOrbCases {
    private static final IronAbility ABILITY=IronAbility.OMINOUS_FIRE_ORB;
    private static LivingEntity actor(GameTestHelper h,int x,int z){var c=h.spawn(EntityType.COW,x,2,z);c.setNoAi(true);c.setNoGravity(true);return c;}
    private static List<OminousFireOrbEntity> orbs(GameTestHelper h,LivingEntity c){return h.getLevel().getEntitiesOfClass(OminousFireOrbEntity.class,c.getBoundingBox().inflate(16));}
    private static void cast(GameTestHelper h,LivingEntity caster,AtomicInteger mana){
        var glyph=(AbilityGlyph)GlyphRegistry.getSpellPart(ABILITY.glyphId());
        var spell=new Spell(MethodTouch.INSTANCE,glyph);
        var source=new LivingCaster(caster){public boolean enoughMana(int n){return mana.get()>=n;}public void expendMana(int n){mana.addAndGet(-n);}};
        var ctx=new SpellContext(h.getLevel(),spell,caster,source,ItemStack.EMPTY);
        ResourceAccount account=new ResourceAccount(){public Object reservationKey(){return mana;}public boolean canSpend(int n){return mana.get()>=n;}public void spend(int n){mana.addAndGet(-n);}};
        var hit=new EntityHitResult(caster);CastContexts.bind(ctx,CastContext.of(ctx,hit,account));
        glyph.onResolve(hit,h.getLevel(),caster,new SpellStats.Builder().build(),ctx,new SpellResolver(ctx));
    }
    public static void placement(GameTestHelper h){
        var caster=actor(h,3,3);var mana=new AtomicInteger(0);cast(h,caster,mana);
        h.assertTrue(orbs(h,caster).isEmpty(),"No unpaid orb");mana.set(ABILITY.mana+100);cast(h,caster,mana);
        var all=orbs(h,caster);h.assertTrue(all.size()==1,"Exactly one real Iron orb");var orb=all.get(0);
        h.assertTrue(orb.getOwner()==caster&&orb.getFuse()==500&&orb.getChargeTime()==20,"Original owner and native fuse/charge data");
        h.assertTrue(orb.getHealth()==150&&orb.getRadius()==12&&orb.getDamage()>=120,"Expected health, radius and scaled base damage");
        h.assertTrue(mana.get()==100,"Creation paid once");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(ABILITY.glyphId()).isPresent(),"Real glyph recipe");
        h.assertTrue(!dev.arsconflux.api.glyph.GlyphAbilities.get(GlyphRegistry.getSpellPart(ABILITY.glyphId())).isEmpty(),"Instinct ability hints");
        h.runAtTickTime(2,()->{h.assertTrue(((OrbClock)orb).ironsNouveau$startedAt()>=0,"Native animation age is synchronized for late trackers");h.succeed();});
    }
    public static void explosion(GameTestHelper h){
        var caster=actor(h,3,3);var friend=actor(h,4,3);var enemy=actor(h,5,3);
        enemy.getAttribute(Attributes.MAX_HEALTH).setBaseValue(500);enemy.setHealth(500);
        var mana=new AtomicInteger(ABILITY.mana);cast(h,caster,mana);var orb=orbs(h,caster).get(0);orb.setNoGravity(true);orb.setChargeTime(0);orb.setFuse(4);
        var allies=Allies.register(dev.ironsnouveau.platform.Locations.id("test:orb_allies"),(a,b)->a==caster&&b==friend);
        h.runAtTickTime(8,()->{try{
            h.assertTrue(orb.isRemoved(),"Native timer detonates and removes orb");
            h.assertTrue(enemy.getHealth()<490,"Native explosion really damaged enemy");
            h.assertTrue(enemy.getActiveEffects().stream().anyMatch(e->e.getDescriptionId().contains("immolate")),"Native immolation stack preserved");
            h.assertTrue(friend.getHealth()==friend.getMaxHealth()&&caster.getHealth()==caster.getMaxHealth(),"Core-only ally and caster take no damage");
            h.assertTrue(mana.get()==0,"Delayed explosion does not pay creation twice");h.succeed();
        }finally{try{allies.close();}catch(Exception e){throw new RuntimeException(e);}}});
    }
    public static void destruction(GameTestHelper h){
        var caster=actor(h,3,3);var enemy=actor(h,5,3);cast(h,caster,new AtomicInteger(ABILITY.mana));var orb=orbs(h,caster).get(0);orb.setNoGravity(true);orb.setChargeTime(0);
        h.runAtTickTime(2,()->{
            orb.onAntiMagic(io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(caster));
            h.assertTrue(orb.getHealth()==140,"Native anti-magic damages the charged orb");
            h.assertTrue(orb.hurt(h.getLevel().damageSources().magic(),200),"Orb accepts enemy damage");
            h.assertTrue(orb.isRemoved(),"Destroyed orb disappears without exploding");
            h.assertTrue(enemy.getHealth()==enemy.getMaxHealth(),"Destruction does not cause native detonation");h.succeed();
        });
    }
    public static void cleanup(GameTestHelper h){
        var caster=actor(h,3,3);var victim=actor(h,5,3);cast(h,caster,new AtomicInteger(ABILITY.mana));var lost=orbs(h,caster).get(0);lost.setNoGravity(true);lost.setChargeTime(0);lost.setFuse(1);caster.discard();lost.tickCount++;lost.tick();
        var other=actor(h,4,4);cast(h,other,new AtomicInteger(ABILITY.mana));var expired=orbs(h,other).stream().filter(e->e!=lost).findFirst().orElseThrow();expired.setNoGravity(true);expired.setChargeTime(0);expired.setFuse(1);
        expired.getPersistentData().getCompound("irons_nouveau_lease").putLong("expires",h.getLevel().getGameTime());
        expired.tickCount++;expired.tick();
        {
            h.assertTrue(lost.isRemoved()&&expired.isRemoved(),"Owner loss and expired lease both clean up");
            h.assertTrue(victim.getHealth()==victim.getMaxHealth()&&other.getHealth()==other.getMaxHealth(),"Invalid lease cannot detonate before end-of-tick cleanup");h.succeed();
        }
    }
    private OminousOrbCases(){}
}
