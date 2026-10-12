package dev.ironsnouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.client.SiphonRayVisuals;
import io.redspace.ironsspellbooks.render.SpellRenderingHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Supply Ars trigger geometry only during an explicit bridge render call; ordinary Iron rays are untouched. */
@Mixin(value = SpellRenderingHelper.class, remap = false)
public abstract class NativeSiphonRenderingMixin {
    @WrapOperation(method = "renderRayOfSiphoning", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/util/Utils;raycastForEntity(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/Entity;FZ)Lnet/minecraft/world/phys/HitResult;"))
    private static HitResult ironsNouveau$endpoint(Level world, Entity entity, float range, boolean blocks, Operation<HitResult> original) {
        var view = SiphonRayVisuals.nativeView();
        // Forge 3.16.3 uses this static helper; NeoForge uses RaycastBuilder.build.
        return view == null ? original.call(world, entity, range, blocks) : view.hit();
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
