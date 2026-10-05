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
    @WrapMethod(method = "applyEffectTick")
    private boolean ironsNouveau$owner(LivingEntity actor, int amplifier, Operation<Boolean> original) {
        String spell = (Object)this instanceof BurningDashEffect ? "burning_dash" : "volt_strike";
        return MotionEffects.tick(actor, spell, () -> original.call(actor, amplifier));
    }
}
