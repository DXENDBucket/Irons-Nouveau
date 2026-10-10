package dev.ironsnouveau.mixin;
import dev.ironsnouveau.client.TelekinesisVisuals;
import io.redspace.ironsspellbooks.render.SpellTargetingLayer;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = SpellTargetingLayer.class, remap = false)
public abstract class TelekinesisTargetColorMixin {
    @Inject(method = "getColor", at = @At("HEAD"), cancellable = true)
    private static void ironsNouveau$color(String spell, CallbackInfoReturnable<Vector3f> ci) {
        var color = TelekinesisVisuals.nativeColor();
        if (color != null) ci.setReturnValue(color);
    }
}
