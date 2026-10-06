package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.StatusBilling;
import io.redspace.ironsspellbooks.effect.ThunderstormEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = ThunderstormEffect.class, remap = false)
public abstract class ThunderstormPulseMixin {
    @WrapMethod(method = "applyEffectTick", remap = true)
    private void ironsNouveau$storm(LivingEntity entity, int amplifier, Operation<Void> original) {
        if (!StatusBilling.pulse(entity, () -> { original.call(entity, amplifier); return true; }))
            entity.removeEffect((net.minecraft.world.effect.MobEffect)(Object)this);
    }
}
