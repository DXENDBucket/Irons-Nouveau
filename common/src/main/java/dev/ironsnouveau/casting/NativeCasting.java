package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.event.SpellCastEvent;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.validation.SpellPhraseValidator;
import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.context.CastContexts;
import dev.ironsnouveau.glyph.NativeFormAugment;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.ArrayList;
import java.util.List;

public final class NativeCasting {
    private NativeCasting() {}

    public static List<SpellValidationError> validate(List<AbstractSpellPart> recipe) {
        var errors = new ArrayList<SpellValidationError>();
        for (var phrase : SpellPhraseValidator.splitSpellIntoPhrases(recipe)) {
            long forms = phrase.getAugments().stream().filter(NativeFormAugment.class::isInstance).count();
            if (forms == 0) {
                // Another Conflux provider owns this payload and its modifier rules.
                if (phrase.getAugments().stream().anyMatch(dev.arsconflux.api.projectile.ProjectileForm.class::isInstance)) continue;
                if (dev.arsconflux.api.projectile.CarrierRegistry.of(phrase.getAction()) != null && phrase.getAugments().stream()
                        .anyMatch(a -> !dev.arsconflux.api.projectile.CarrierRegistry.of(phrase.getAction()).originallySupported(a)
                                && (a == AugmentAmplify.INSTANCE || a == AugmentDampen.INSTANCE)))
                    errors.add(new Error(phrase.getFirstPosition(), phrase.getAction(), "requires_form"));
                continue;
            }
            var form = (NativeFormAugment) phrase.getAugments().stream().filter(NativeFormAugment.class::isInstance).findFirst().orElseThrow();
            var profile = dev.arsconflux.api.projectile.CarrierRegistry.of(phrase.getAction());
            String reason = !form.adapter().supportsMethod(phrase.getAction()) || profile == null
                    || (phrase.getFirstPosition() != 0 && profile != CarrierProfiles.ORBIT.core() && !profile.propagator())
                    ? "requires_projectile" : forms != 1 ? "one_form" : null;
            if (reason == null && phrase.getAugments().stream().anyMatch(a -> !(a instanceof NativeFormAugment) && !profile.augments().contains(a)))
                reason = "unsupported_modifier";
            if (reason == null && phrase.getAugments().stream().filter(NativeFormAugment.class::isInstance).anyMatch(a -> !a.isEnabled()))
                reason = "disabled";
            if (reason != null) errors.add(new Error(phrase.getFirstPosition(), phrase.getAction(), reason));
        }
        return errors;
    }
    public static NativeFormAugment form(Spell spell, LivingEntity caster) {
        return spell.getAugments(0, caster).stream().filter(NativeFormAugment.class::isInstance)
                .map(NativeFormAugment.class::cast).findFirst().orElse(null);
    }
    public static CastModifiers modifiers(Spell spell, LivingEntity caster, double speed) {
        return modifiers(spell.getAugments(0, caster), speed);
    }
    private static CastModifiers modifiers(List<AbstractAugment> augments, double speed) {
        return new CastModifiers(SpellLevels.delta(augments), speed);
    }
    public static int level(NativeFormAugment form, CastModifiers modifiers, LivingEntity caster) {
        return modifiers.resolveLevel(dev.ironsnouveau.progression.SpellProgress.baseLevel(caster, form.spellId()));
    }
    public static void beforeCast(SpellCastEvent event) {
        PresetTools.scoped(event.context, () -> { beforeCastScoped(event); return null; });
    }
    private static void beforeCastScoped(SpellCastEvent event) {
        var caster = event.context.getUnwrappedCaster();
        if (caster == null || caster.level().isClientSide) return;
        var error = validate(dev.arsconflux.api.glyph.ArsSpellAccess.parts(event.spell));
        boolean allowed = error.isEmpty();
        boolean hasForm = false;
        for (var phrase : SpellPhraseValidator.splitSpellIntoPhrases(dev.arsconflux.api.glyph.ArsSpellAccess.parts(event.spell))) {
            for (var augment : phrase.getAugments()) if (augment instanceof NativeFormAugment form) {
                hasForm = true;
                allowed &= SpellRegistry.getSpell(form.spellId()).isEnabled()
                        && GlyphAccessEvent.allowed(caster, form.getRegistryName(), form.spellId(),
                        level(form, modifiers(phrase.getAugments(), 1), caster), GlyphAccessEvent.Action.RESOLVE);
            }
        }
        // Effects use their own phrase augments, independently of a preceding projectile form.
        for (var phrase : SpellPhraseValidator.splitSpellIntoPhrases(dev.arsconflux.api.glyph.ArsSpellAccess.parts(event.spell))) {
            if (phrase.getAction() instanceof dev.ironsnouveau.glyph.BridgeGlyph glyph) {
                hasForm = true;
                allowed &= glyph.permitted(caster, SpellLevels.resolve(caster, glyph.definition().spellId(), phrase.getAugments()), GlyphAccessEvent.Action.RESOLVE);
            }
        }
        if (!hasForm) return;
        if (!allowed) {
            event.setCanceled(true);
            if (caster instanceof Player player && !caster.level().isClientSide)
                player.displayClientMessage(error.isEmpty() ? Component.translatable("irons_nouveau.cast.denied") : error.get(0).makeTextComponentExisting(), true);
        }
    }
    /** Runs at Ars' addFreshEntity call, after velocity, spread and split count have been calculated. */
    public static boolean convert(Entity entity) {
        if (!(entity instanceof EntityProjectileSpell ars) || dev.arsconflux.api.glyph.ArsSpellAccess.resolver(ars) == null) return false;
        var resolver = dev.arsconflux.api.glyph.ArsSpellAccess.resolver(ars);
        var context = CastContext.of(resolver.spellContext, null, null);
        var caster = context.caster();
        if (caster == null) return false;
        var form = form(resolver.spell, caster);
        if (form == null) return false;
        var profile = dev.arsconflux.api.projectile.CarrierRegistry.of(ars);
        if (profile == null) return false;
        if (!(ars.level() instanceof ServerLevel world)) return true;
        var augments = resolver.spell.getAugments(0, caster);
        if (augments.stream().filter(NativeFormAugment.class::isInstance).count() != 1
                || augments.stream().anyMatch(a -> !(a instanceof NativeFormAugment) && !profile.augments().contains(a))) return true;
        var velocity = ars.getDeltaMovement();
        var modifiers = modifiers(resolver.spell, caster, profile.arsDriven() ? 1 : velocity.length() / .75);
        int level = level(form, modifiers, caster);
        var spell = SpellRegistry.getSpell(form.spellId());
        var plan = new CastPlan(form.getRegistryName(), form.spellId(), level,
                spell.getSpellPower(level, caster), modifiers, Math.max(1, ars.getExpirationTime() + 1));
        var mana = TriggerMana.of(context);
        context = context.withAccount(mana.account()).withIncomingDirection(velocity)
                .withTarget(net.minecraft.world.phys.BlockHitResult.miss(ars.position(), net.minecraft.core.Direction.UP,
                        net.minecraft.core.BlockPos.containing(ars.position())));
        CastContexts.bind(resolver.spellContext, context);
        var frozen = resolver.clone();
        CastExecution execution = form.adapter().createExecution(world, caster);
        if (profile.arsDriven()) execution = ((ProjectileExecution)execution).withTrajectory(ars);
        ImpactContinuation continuation = hit -> {
            var child = frozen.clone();
            CastContexts.bind(child.spellContext, CastContexts.current());
            child.onResolveEffect(world, hit);
        };
        if (!profile.arsDriven()) continuation = new OncePerTargetContinuation(continuation);
        var session = new CastSession(world, context, plan,
                AimSource.fixed(new CastAim(ars.position(), velocity.normalize(), null)),
                execution, continuation).billing(TriggerMana.of(context), profile.repeated());
        CastSessions.start(session);
        if (!profile.arsDriven()) ars.discard();
        return true;
    }
    private record Error(int position, AbstractSpellPart spellPart, String reason) implements SpellValidationError {
        @Override public int getPosition() { return position; }
        @Override public AbstractSpellPart getSpellPart() { return spellPart; }
        @Override public MutableComponent makeTextComponentExisting() { return Component.translatable("irons_nouveau.validation." + reason); }
        @Override public MutableComponent makeTextComponentAdding() { return makeTextComponentExisting(); }
    }
}
