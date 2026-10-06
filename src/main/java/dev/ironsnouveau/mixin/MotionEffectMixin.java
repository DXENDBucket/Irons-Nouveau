package dev.ironsnouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.MotionEffects;
import io.redspace.ironsspellbooks.effect.BurningDashEffect;
import io.redspace.ironsspellbooks.effect.VoltStrikeEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {BurningDashEffect.class, VoltStrikeEffect.class}, remap = false)
public abstract class MotionEffectMixin {
    @WrapMethod(method = "applyEffectTick", remap = true)
    private void ironsNouveau$owner(LivingEntity actor, int amplifier, Operation<Void> original) {
        String spell = (Object)this instanceof BurningDashEffect ? "burning_dash" : "volt_strike";
        if (!MotionEffects.tick(actor, spell, () -> { original.call(actor, amplifier); return true; }))
            actor.removeEffect((net.minecraft.world.effect.MobEffect)(Object)this);
    }
}
