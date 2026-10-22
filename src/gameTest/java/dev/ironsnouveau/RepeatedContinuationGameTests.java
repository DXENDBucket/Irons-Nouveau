package dev.ironsnouveau;

import dev.ironsnouveau.casting.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("irons_nouveau_repeat_continuation") @PrefixGameTestTemplate(false)
public final class RepeatedContinuationGameTests {
    @GameTest(template="empty",timeoutTicks=35,batch="repeat_ball_continuation")
    public static void sameBallContinuesAgainAfterSuccessfulDamage(GameTestHelper h) {
        EmitterMotionGameTests.ballRepeatsRequireManaAndContinue(h,h::succeed);
    }

    /** Controlled native damage callback: verifies its scope, not a complete fireball explosion. */
    @GameTest(template="empty",timeoutTicks=20,batch="complex_callback_continuation")
    public static void complexDamageDeduplicatesOnlyWithinOneCallback(GameTestHelper h) {
        var caster=h.spawn(EntityType.COW,1,2,1);
        var victim=h.spawn(EntityType.COW,5,2,4);
        caster.setNoAi(true);caster.setNoGravity(true);victim.setNoAi(true);victim.setNoGravity(true);
        victim.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);victim.setHealth(1000);
        victim.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        var id=dev.ironsnouveau.platform.Locations.id("irons_spellbooks","fireball");
        var spell=SpellRegistry.getSpell(id);var payload=new ComplexProjectilePayload("fireball");
        var count=new AtomicInteger();
        var plan=new CastPlan(dev.ironsnouveau.platform.Locations.id("irons_nouveau","glyph_fireball"),
                id,1,spell.getSpellPower(1,caster),new CastModifiers(0,1),100);
        var session=new CastSession(h.getLevel(),caster,plan,
                AimSource.fixed(new CastAim(caster.getEyePosition(),new Vec3(1,0,0),null)),
                new CastExecution() {
                    public boolean start(CastSession s){return true;}
                    public boolean tick(CastSession s){return false;}
                    public void close(CastSession s,CastSession.EndReason reason){}
                },hit->count.incrementAndGet());
        var shot=payload.create(h.getLevel(),caster);
        shot.setOwner(caster);shot.setPos(victim.position());
        ((NativeCastCarrier)shot).ironsNouveau$session(session);
        try {
            h.assertTrue(session.start(),"Managed native complex session starts");
            var source=spell.getDamageSource(shot,caster);var hit=new EntityHitResult(victim);
            ComplexImpacts.execute(shot,hit,()->{
                h.assertTrue(DamageSources.applyDamage(victim,2,source),"First native damage succeeds");
                victim.invulnerableTime=0;
                h.assertTrue(DamageSources.applyDamage(victim,2,source),"Second damage inside the same callback succeeds");
                ComplexImpacts.statusApplied(victim); // Damage plus status is still one native callback.
            });
            h.assertTrue(victim.getHealth()==996 && count.get()==1,"Two real damages and a status notification continue once in their shared scope");
            victim.invulnerableTime=0;
            ComplexImpacts.execute(shot,hit,()->h.assertTrue(DamageSources.applyDamage(victim,2,source),"A later callback succeeds"));
            h.assertTrue(victim.getHealth()==994 && count.get()==2,"The same projectile/target can continue in a later callback");
            ComplexImpacts.execute(shot,hit,()->h.assertTrue(!DamageSources.applyDamage(victim,2,source),"Immediate damage is rejected by invulnerable frames"));
            h.assertTrue(victim.getHealth()==994 && count.get()==2,"A failed complex damage callback does not continue");
        } finally {
            session.finish(CastSession.EndReason.COMPLETED);shot.discard();caster.discard();victim.discard();
        }
        h.succeed();
    }
}
