package dev.ironsnouveau.mixin;
import dev.ironsnouveau.casting.*;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Fire's ignition runs before the superclass tick; gate it too, using the paid emission origin. */
@Mixin(value = FireBreathProjectile.class, remap = false)
public abstract class ManagedFireBreathMixin {
    @Inject(method = "tick", remap = true, at = @At("HEAD"), cancellable = true)
    private void ironsNouveau$managed(CallbackInfo ci) {
        var cone = (FireBreathProjectile)(Object)this;
        if (!((ConeState)cone).ironsNouveau$managed()) return;
        if (cone.level().isClientSide) BreathVisuals.tick(cone);
        ci.cancel();
    }
}
