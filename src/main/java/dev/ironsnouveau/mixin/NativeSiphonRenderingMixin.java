package dev.ironsnouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.client.SiphonRayVisuals;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.render.SpellRenderingHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Supply Ars trigger geometry only during an explicit bridge render call; ordinary Iron rays are untouched. */
@Mixin(value = SpellRenderingHelper.class, remap = false)
public abstract class NativeSiphonRenderingMixin {
    @WrapOperation(method = "renderRayOfSiphoning", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/util/RaycastBuilder;build()Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult ironsNouveau$endpoint(RaycastBuilder builder, Operation<HitResult> original) {
        var view = SiphonRayVisuals.nativeView();
        return view == null ? original.call(builder) : view.hit();
    }
    @WrapOperation(method = "renderRayOfSiphoning", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getEyePosition()Lnet/minecraft/world/phys/Vec3;", remap = true))
    private static Vec3 ironsNouveau$origin(LivingEntity entity, Operation<Vec3> original) {
        var view = SiphonRayVisuals.nativeView();
        return view == null ? original.call(entity) : view.origin();
    }
    @WrapOperation(method = "renderRayOfSiphoning", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getEyePosition(F)Lnet/minecraft/world/phys/Vec3;", remap = true))
    private static Vec3 ironsNouveau$interpolatedOrigin(LivingEntity entity, float partial, Operation<Vec3> original) {
        var view = SiphonRayVisuals.nativeView();
        return view == null ? original.call(entity, partial) : view.origin();
    }
}
