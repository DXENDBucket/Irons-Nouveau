package dev.ironsnouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.ironsnouveau.casting.ConeState;
import io.redspace.ironsspellbooks.entity.spells.electrocute.*;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Preserve all native lightning layers and branches, using the emitting cone's Ars pose. */
@Mixin(value = ElectrocuteRenderer.class, remap = false)
public abstract class NativeElectrocuteRenderingMixin {
    @WrapOperation(method = "render(Lio/redspace/ironsspellbooks/entity/spells/electrocute/ElectrocuteProjectile;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/entity/spells/electrocute/ElectrocuteProjectile;getOwner()Lnet/minecraft/world/entity/Entity;"))
    private Entity ironsNouveau$renderPose(ElectrocuteProjectile cone, Operation<Entity> original) {
        // The render-only owner supplies facing. Damage ownership remains untouched.
        // This also keeps remote lightning visible when the real owner is outside tracking range.
        return ((ConeState)cone).ironsNouveau$managed() ? cone : original.call(cone);
    }
    @WrapOperation(method = "render(Lio/redspace/ironsspellbooks/entity/spells/electrocute/ElectrocuteProjectile;FFLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;translate(DDD)V"))
    private void ironsNouveau$scale(PoseStack poses, double x, double y, double z, Operation<Void> original,
                                     @Local(argsOnly = true) ElectrocuteProjectile cone) {
        original.call(poses, x, y, z);
        if (((ConeState)cone).ironsNouveau$managed()) {
            float scale = ((ConeState)cone).ironsNouveau$scale(); poses.scale(scale, scale, scale);
        }
    }
}
