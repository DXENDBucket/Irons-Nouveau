package dev.ironsnouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.client.CastingMovementState;
import io.redspace.ironsspellbooks.player.ClientPlayerEvents;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Broaden only Iron's movement predicate; the native formula runs once, even during overlapping casts. */
@Mixin(value = ClientPlayerEvents.class, remap = false)
public abstract class NativeCastingMovementMixin {
    @WrapOperation(method = "onCalculatePlayerSpeed", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/player/ClientMagicData;isCasting()Z"))
    private static boolean ironsNouveau$movement(Operation<Boolean> original) {
        return original.call() || CastingMovementState.active();
    }
}
