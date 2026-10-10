package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.SpellAdapter;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.spells.holy.HasteSpell;
import io.redspace.ironsspellbooks.spells.evocation.SlowSpell;
import io.redspace.ironsspellbooks.spells.nature.BlightSpell;
import net.minecraft.world.effect.MobEffect;
import java.util.function.ToIntFunction;

/** Ars provides the target. Native public formulas provide the status; no MagicData target mutation. */
public final class TargetedSpellAdapters {
    private TargetedSpellAdapters() {}
    public static final SpellAdapter HASTE = status(MobEffectRegistry.HASTENED,
            ctx -> ((HasteSpell)ctx.spell()).getDuration(ctx.level(), ctx.caster()),
            ctx -> ((HasteSpell)ctx.spell()).getAmplifier(ctx.level(), ctx.caster()));
    public static final SpellAdapter SLOW = status(MobEffectRegistry.SLOWED,
            ctx -> ((SlowSpell)ctx.spell()).getDuration(ctx.level(), ctx.caster()),
            ctx -> ((SlowSpell)ctx.spell()).getAmplifier(ctx.level(), ctx.caster()));
    public static final SpellAdapter BLIGHT = status(MobEffectRegistry.BLIGHT,
            ctx -> ((BlightSpell)ctx.spell()).getDuration(ctx.level(), ctx.caster()),
            ctx -> ((BlightSpell)ctx.spell()).getAmplifier(ctx.level(), ctx.caster()));
    private static SpellAdapter status(java.util.function.Supplier<MobEffect> effect, ToIntFunction<Resolution> ticks,
                                       ToIntFunction<Resolution> amplifier) {
        return (ctx, target) -> {
            int duration = (int)net.minecraft.util.Mth.clamp(ticks.applyAsInt(ctx) * ctx.duration(), 0, Integer.MAX_VALUE);
            if (duration == 0) return false;
            return target.addEffect(dev.ironsnouveau.platform.Effects.create(effect, duration, net.minecraft.util.Mth.clamp(amplifier.applyAsInt(ctx), 0, 255),
                    false, false, true), ctx.caster());
        };
    }
}
