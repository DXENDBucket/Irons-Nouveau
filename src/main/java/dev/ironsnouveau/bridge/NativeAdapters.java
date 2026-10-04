package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.SpellAdapter;
import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.effect.OakskinData;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.world.effect.MobEffectInstance;
import net.neoforged.neoforge.common.NeoForge;

/** Explicit adapters are intentional: native onCast assumes native aim, state, timing and mana. */
public final class NativeAdapters {
    private NativeAdapters() {}

    public static final SpellAdapter HEAL = (ctx, target) -> {
        float amount = (float) ctx.power();
        if (amount <= 0 || target.getHealth() >= target.getMaxHealth()) return false;
        NeoForge.EVENT_BUS.post(new SpellHealEvent(ctx.caster(), target, amount, ctx.spell().getSchoolType()));
        target.heal(amount);
        return true;
    };
    public static final SpellAdapter FORTIFY = (ctx, target) -> {
        if (ctx.power() < 1) return false;
        return target.addEffect(new MobEffectInstance(MobEffectRegistry.FORTIFY,
                (int) (2400 * ctx.duration()), Math.clamp((int) ctx.power() - 1, 0, 255),
                false, false, true), ctx.caster());
    };
    public static final SpellAdapter OAKSKIN = (ctx, target) -> {
        if (ctx.power() <= 0) return false;
        target.removeEffect(MobEffectRegistry.OAKSKIN);
        OakskinData.remove(target);
        return target.addEffect(new MobEffectInstance(MobEffectRegistry.OAKSKIN,
                Math.max(1, (int) (ctx.power() * 20 * ctx.duration())), 2,
                false, false, true), ctx.caster());
    };
}
