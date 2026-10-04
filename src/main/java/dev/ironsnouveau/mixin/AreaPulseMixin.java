package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value = AoeEntity.class, remap = false)
public abstract class AreaPulseMixin {
    @WrapOperation(method = "checkHits", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/entity/spells/AoeEntity;applyEffect(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void ironsNouveau$area(AoeEntity field, LivingEntity target, Operation<Void> original) {
        EffectResources.pulse(field, () -> { original.call(field, target); return true; });
    }
}
