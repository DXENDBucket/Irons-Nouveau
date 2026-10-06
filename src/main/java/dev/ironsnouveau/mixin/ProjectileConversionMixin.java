package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.NativeCasting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = MethodProjectile.class, remap = false)
public abstract class ProjectileConversionMixin {
    @WrapOperation(method = "summonProjectiles", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private boolean ironsNouveau$convert(Level world, Entity entity, Operation<Boolean> original) {
        return NativeCasting.convert(entity) || original.call(world, entity);
    }
}
