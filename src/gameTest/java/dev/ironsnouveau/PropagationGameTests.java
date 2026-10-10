package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import com.hollingsworth.arsnouveau.common.spell.validation.StandardSpellValidator;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.glyph.NativeFormAugment;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("irons_nouveau_propagation") @PrefixGameTestTemplate(false)
public final class PropagationGameTests {
    private static NativeFormAugment form() {
        return IronsNouveau.forms().stream().filter(f -> f.spellId().getPath().equals("firebolt")).findFirst().orElseThrow();
    }
    private static LivingEntity cow(GameTestHelper h) {
        var caster = h.spawn(EntityType.COW, 2, 3, 2);
        caster.setNoAi(true); caster.setNoGravity(true); caster.setYRot(-90); return caster;
    }
    private static SpellResolver resolver(GameTestHelper h, LivingEntity caster, Spell spell, AtomicInteger balance) {
        var source = new LivingCaster(caster) {
            @Override public boolean enoughMana(int cost) { return balance.get() >= cost; }
            @Override public void expendMana(int cost) { balance.addAndGet(-cost); }
        };
        return new SpellResolver(new SpellContext(h.getLevel(), spell, caster, source, ItemStack.EMPTY));
    }
    private static List<AbstractMagicProjectile> shots(GameTestHelper h, LivingEntity caster) {
        return h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, caster.getBoundingBox().inflate(32), p -> p.getOwner() == caster);
    }
    private static void close(AbstractMagicProjectile shot) {
        ((NativeCastCarrier)shot).ironsNouveau$session().finish(CastSession.EndReason.COMPLETED);
    }
    @GameTest(template="empty", timeoutTicks=50)
    public static void allPropagatorsReplaceAndPreserveRemainingEffects(GameTestHelper h) {
        var caster = cow(h); var target = h.spawn(EntityType.COW, 7, 3, 2); target.setNoAi(true);
        var balance = new AtomicInteger(100000);
        for (var profile : CarrierProfiles.values()) {
            if (!profile.propagator() || profile.glyph() == null) continue;
            // The native child adds a synthetic Dampen. It must not lower the real Iron level.
            var spell = new Spell(MethodSelf.INSTANCE, profile.glyph(), form(), AugmentAmplify.INSTANCE,
                    AugmentExtract.INSTANCE, EffectHarm.INSTANCE);
            h.assertTrue(new StandardSpellValidator(false).validate(spell.unsafeList()).isEmpty(), "Editor accepts " + profile);
            var resolver = resolver(h, caster, spell, balance);
            int before = balance.get();
            h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "Real propagator cast accepted: " + profile);
            var shots = shots(h, caster);
            h.assertTrue(shots.size() == 1, "Exactly one native Iron carrier from " + profile + ": " + shots.size());
            var shot = shots.getFirst(); var session = ((NativeCastCarrier)shot).ironsNouveau$session();
            h.assertTrue(session.execution() instanceof ArsTrajectoryExecution, "Retains optional motion for " + profile);
            var trajectory = ((ArsTrajectoryExecution)session.execution()).trajectory();
            h.assertTrue(CarrierProfiles.of(trajectory) == profile, "Child carrier retains propagator capabilities: " + profile);
            h.assertTrue(session.plan().spellLevel() == 2, "Real amplification only; synthetic placeholder is ignored");
            h.assertTrue(before - balance.get() == resolver.getExpendedCost()
                    + SpellRegistry.getSpell(form().spellId()).getManaCost(2), "Upfront Ars and one native trigger payment");
            float health = target.getHealth(); session.impact(new EntityHitResult(target));
            h.assertTrue(target.getHealth() < health, "Remaining Ars damage runs after Iron impact");
            target.setHealth(target.getMaxHealth()); target.invulnerableTime = 0;
            close(shot);
            h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class,
                    caster.getBoundingBox().inflate(32)).isEmpty(), "No duplicate original carrier");
        }
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=50)
    public static void nativeImpactPropagatesAnotherNativeProjectile(GameTestHelper h) {
        var profile = CarrierProfiles.PROPAGATE_STRAIGHT;
        if (profile.glyph() == null) { h.succeed(); return; }
        var caster = cow(h); var balance = new AtomicInteger(100000);
        var spell = new Spell(MethodProjectile.INSTANCE, form(), profile.glyph(), form(), EffectHarm.INSTANCE);
        h.assertTrue(new StandardSpellValidator(false).validate(spell.unsafeList()).isEmpty(), "Two native forms in separate phrases are valid");
        h.assertTrue(resolver(h, caster, spell, balance).onCast(ItemStack.EMPTY, h.getLevel()), "Initial cast succeeds");
        var first = shots(h, caster).getFirst();
        var pos = caster.position().add(4, 0, 0);
        var hit = new BlockHitResult(pos, Direction.UP, net.minecraft.core.BlockPos.containing(pos), false);
        ((dev.ironsnouveau.mixin.NativeProjectileHitAccess)first).ironsNouveau$hit(hit);
        var children = shots(h, caster).stream().filter(s -> s != first).toList();
        h.assertTrue(children.size() == 1, "Native collision emits a second native shot");
        var child = children.getFirst();
        h.assertTrue(child.position().distanceTo(pos.add(0, 1, 0)) < .01, "Second shot uses real propagation origin");
        close(child); if (!first.isRemoved()) close(first);
        h.succeed();
    }
    @GameTest(template="empty", timeoutTicks=80)
    public static void echoingTrailEmitsNativeShotsRepeatedly(GameTestHelper h) {
        if (CarrierProfiles.TRAIL.glyph() == null || CarrierProfiles.PROPAGATE_STRAIGHT.glyph() == null) { h.succeed(); return; }
        var caster = cow(h); var balance = new AtomicInteger(100000);
        var spell = new Spell(CarrierProfiles.TRAIL.glyph(), AugmentSensitive.INSTANCE,
                AugmentDecelerate.INSTANCE, AugmentDecelerate.INSTANCE,
                AugmentExtendTime.INSTANCE, AugmentExtendTime.INSTANCE, AugmentExtendTime.INSTANCE,
                CarrierProfiles.PROPAGATE_STRAIGHT.glyph(), form());
        h.assertTrue(new StandardSpellValidator(false).validate(spell.unsafeList()).isEmpty(), "Trail followed by propagation is valid");
        h.assertTrue(resolver(h, caster, spell, balance).onCast(ItemStack.EMPTY, h.getLevel()), "Actual trail cast succeeds");
        var trails = h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, caster.getBoundingBox().inflate(8));
        h.assertTrue(trails.size() == 1, "One original trail carrier spawned");
        var trail = trails.getFirst();
        var center = caster.chunkPosition();
        for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) h.getLevel().setChunkForced(center.x+x, center.z+z, true);
        h.succeedWhen(() -> {
            var children = shots(h, caster);
            h.assertTrue(children.size() >= 2, "Two distinct real trail pulses emit native shots: " + children.size()
                    + " trailTicks=" + trail.tickCount + " age=" + trail.age + " removed=" + trail.isRemoved()
                    + " pos=" + trail.position() + " recipe=" + trail.resolver().spell);
            h.assertTrue(children.stream().allMatch(s -> ((NativeCastCarrier)s).ironsNouveau$session().execution() instanceof ArsTrajectoryExecution),
                    "Propagated carriers preserve Ars trajectory");
            children.forEach(PropagationGameTests::close);
            for (int x = -1; x <= 1; x++) for (int z = -1; z <= 1; z++) h.getLevel().setChunkForced(center.x+x, center.z+z, false);
        });
    }
}
