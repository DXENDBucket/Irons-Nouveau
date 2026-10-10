package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.resource.*;
import dev.ironsnouveau.casting.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("irons_nouveau_context") @PrefixGameTestTemplate(false)
public final class CastContextGameTests {
    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        var entity = h.spawn(EntityType.COW, x, 2, z);
        entity.setNoAi(true); entity.setNoGravity(true); entity.setYRot(-90);
        entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); entity.setHealth(100);
        entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); return entity;
    }
    private static ResourceAccount account(AtomicInteger balance) {
        return new ResourceAccount() {
            public Object reservationKey() { return balance; }
            public boolean canSpend(int amount) { return balance.get() >= amount; }
            public void spend(int amount) { balance.addAndGet(-amount); }
        };
    }
    private static SpellContext ars(GameTestHelper h, LivingEntity caster, Spell spell) {
        return new SpellContext(h.getLevel(), spell, caster, new LivingCaster(caster) {
            public boolean enoughMana(int amount) { return amount == 0; }
            public void expendMana(int amount) { if (amount != 0) throw new AssertionError("Must use explicit account, not wrapped caster mana"); }
        }, ItemStack.EMPTY);
    }
    @GameTest(template="empty", timeoutTicks=20)
    public static void cloneIsolationScopesAndProxyLoss(GameTestHelper h) {
        var caster = cow(h, 1, 1); var host = cow(h, 3, 1); var credit = cow(h, 5, 1);
        var pool = account(new AtomicInteger(100));
        var ars = ars(h, caster, new Spell(MethodProjectile.INSTANCE));
        var frame = CastContext.of(ars, new EntityHitResult(host), pool).withExecutor(host).withDamageOwner(credit)
                .withIncomingDirection(new Vec3(1, -.5, 0));
        CastContexts.bind(ars, frame);
        var child = ars.clone(); var sibling = ars.clone();
        var next = CastContext.of(child, new EntityHitResult(credit), null);
        h.assertTrue(next.ars() == child && next.caster() == caster && next.executor() == caster
                && next.damageOwner() == credit && next.account() == pool, "Clone retains chain identity/account but resets per-trigger executor");
        h.assertTrue(next.origin().equals(credit.position()) && next.incomingDirection().dot(frame.incomingDirection()) > .999,
                "New hit location and previous flight direction coexist");
        CastContexts.bind(child, next.withDamageOwner(host));
        h.assertTrue(CastContext.of(sibling, null, null).damageOwner() == credit, "Sibling attachment maps are independent");
        CastContexts.run(frame, () -> {
            try { CastContexts.run(next, () -> { throw new IllegalStateException("fixture"); }); }
            catch (IllegalStateException expected) { h.assertTrue(CastContexts.current() == frame, "Nested exceptional scope restores caller"); }
        });
        h.assertTrue(CastContexts.current() == null, "Scope leaves no ambient cast context");
        var closed = new AtomicInteger();
        var session = new ExecutionSession<>(h.getLevel(), frame, 0, 10, new ExecutionDriver<Integer>() {
            public boolean start(ExecutionSession<Integer> s) { h.assertTrue(CastContexts.current() == frame, "Driver starts in unified context"); return true; }
            public boolean tick(ExecutionSession<Integer> s) { h.assertTrue(CastContexts.current() == frame, "Deferred tick restores context"); return false; }
            public void close(ExecutionSession<Integer> s, ExecutionSession.EndReason reason) { closed.incrementAndGet(); }
        }, hit -> {}, () -> CastContexts.current().caster() == caster, ExecutionScope.DIRECT);
        h.assertTrue(session.start(), "Proxy session starts"); session.tick(); host.discard(); session.tick();
        h.assertTrue(!session.active() && session.state() == ExecutionSession.State.CANCELLED && closed.get() == 1,
                "Losing actual executor terminates session once");
        h.assertTrue(CastContexts.current() == null, "Tick and cleanup restore scope"); h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=30)
    public static void nativeBreathKeepsOriginalPowerCustomAccountAndSeparateDamageOwner(GameTestHelper h) {
        var caster = cow(h, 1, 1); var host = cow(h, 2, 4); var victim = cow(h, 5, 4); var credit = cow(h, 1, 7);
        h.runAtTickTime(2, () -> {
            var glyph = IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals("poison_breath")).findFirst().orElseThrow();
            var nativeSpell = (io.redspace.ironsspellbooks.spells.nature.PoisonBreathSpell)SpellRegistry.getSpell(glyph.definition().spellId());
            int cost = nativeSpell.getManaCost(1);
            var balance = new AtomicInteger(cost * 2); var pool = account(balance);
            var ars = ars(h, caster, new Spell(MethodTouch.INSTANCE, glyph));
            CastContexts.bind(ars, CastContext.of(ars, null, pool).withDamageOwner(credit));
            glyph.onResolve(new EntityHitResult(host), h.getLevel(), caster, new SpellStats.Builder().build(), ars, new SpellResolver(ars));
            var cones = h.getLevel().getEntitiesOfClass(AbstractConeProjectile.class, host.getBoundingBox().inflate(20), c -> c.getOwner() == credit);
            h.assertTrue(cones.size() == 1, "Native cone uses damage owner, not host"); var cone = cones.get(0);
            h.assertTrue(((ConeState)cone).ironsNouveau$anchor() == host.getId() && host.getHealth() == 100,
                    "Actual executor owns pose and is excluded from damage");
            h.assertTrue(Math.abs((100 - victim.getHealth()) - nativeSpell.getDamage(1, caster)) < .001,
                    "Original caster still supplies power");
            h.assertTrue(victim.getHealth() < 100 && victim.getLastDamageSource().getEntity() == credit && balance.get() == cost,
                    "Damage credit and first payment use explicit context roles");
            h.runAtTickTime(18, () -> {
                h.assertTrue(balance.get() == 0 && cone.isRemoved(), "Deferred upkeep retains custom pool and stops on empty mana");
                var frostHost = cow(h, 3, 8); var frostTarget = cow(h, 7, 8);
                var frost = IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals("ray_of_frost")).findFirst().orElseThrow();
                balance.set(SpellRegistry.getSpell(frost.definition().spellId()).getManaCost(1));
                var frostArs = ars(h, caster, new Spell(MethodTouch.INSTANCE, frost));
                CastContexts.bind(frostArs, CastContext.of(frostArs, null, pool).withDamageOwner(credit));
                frost.onResolve(new EntityHitResult(frostHost), h.getLevel(), caster, new SpellStats.Builder().build(), frostArs, new SpellResolver(frostArs));
                h.assertTrue(frostTarget.getHealth() < 100 && frostTarget.getLastDamageSource().getEntity() == credit
                        && frostTarget.getTicksFrozen() > 0 && balance.get() == 0,
                        "Native onCast retains damage owner, freeze metadata and explicit account");
                h.assertTrue(CastContexts.current() == null, "Deferred native cast leaves no scope behind"); h.succeed();
            });
        });
    }
    @GameTest(template="empty", timeoutTicks=20)
    public static void nativeProjectileContinuationRetainsOwnershipAccountAndHitDirection(GameTestHelper h) {
        var caster = cow(h, 1, 1); var credit = cow(h, 1, 6); var victim = cow(h, 5, 1);
        var balance = new AtomicInteger(10000); var pool = account(balance); var seen = new CastContext[1];
        var capture = new AbstractEffect(dev.arsconflux.api.projectile.CarrierRegistry.id("context_test:capture"), "capture") {
            public boolean isEnabled() { return true; }
            public int getDefaultManaCost() { return 0; }
            public int getCastingCost() { return 0; }
            public SpellTier defaultTier() { return SpellTier.ONE; }
            protected java.util.Set<AbstractAugment> getCompatibleAugments() { return java.util.Set.of(); }
            public void onResolve(HitResult hit, net.minecraft.world.level.Level world, LivingEntity actor, SpellStats stats, SpellContext context, SpellResolver resolver) {
                seen[0] = CastContext.of(context, hit, null);
            }
        };
        var form = IronsNouveau.forms().stream().filter(f -> f.spellId().getPath().equals("magic_missile")).findFirst().orElseThrow();
        var ars = ars(h, caster, new Spell(MethodProjectile.INSTANCE, form, capture));
        ars.setCurrentIndex(1); // Ars has consumed the cast method when it constructs its projectile.
        CastContexts.bind(ars, CastContext.of(ars, null, pool).withDamageOwner(credit));
        var carrier = new EntityProjectileSpell(h.getLevel(), new SpellResolver(ars));
        carrier.setPos(caster.getEyePosition()); carrier.setDeltaMovement(.75, .15, 0);
        h.assertTrue(NativeCasting.convert(carrier), "Iron takes over original carrier");
        var shot = h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, caster.getBoundingBox().inflate(16), p -> p.getOwner() == credit).get(0);
        var session = ((NativeCastCarrier)shot).ironsNouveau$session();
        h.assertTrue(session.caster() == caster && session.damageOwner() == credit && session.context().account() == pool,
                "Session shares common roles and account");
        h.assertTrue(ProjectilePayload.HALF_POWER.apply(session, shot, victim) && victim.getLastDamageSource().getEntity() == credit,
                "Native impact uses damage credit while power remains caster-derived");
        session.impact(new EntityHitResult(victim));
        h.assertTrue(seen[0] != null && seen[0].caster() == caster && seen[0].damageOwner() == credit
                && seen[0].executor() == caster && seen[0].account() == pool
                && ((EntityHitResult)seen[0].target()).getEntity() == victim, "Continuation receives original roles, fresh target and account: seen=" + seen[0] + ", pool=" + pool + ", caster=" + caster + ", credit=" + credit);
        h.assertTrue(seen[0].incomingDirection().dot(shot.getDeltaMovement().normalize()) > .999, "Continuation captures actual incoming direction");
        h.assertTrue(balance.get() == 10000 - SpellRegistry.getSpell(form.spellId()).getManaCost(1), "Single projectile creation paid once");
        session.finish(CastSession.EndReason.COMPLETED); h.assertTrue(CastContexts.current() == null, "Continuation and cleanup restore scope"); h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=100)
    public static void existingNestedPropagationStillWorks(GameTestHelper h) {
        PropagationGameTests.nativeImpactPropagatesAnotherNativeProjectile(h);
    }
}
