package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.common.items.SpellBow;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.ActiveCooldowns;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = SpellBow.class, remap = false)
public abstract class BowCooldownSpawnMixin {
    @WrapOperation(method = "addArrow", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    private boolean ironsNouveau$spawn(Level world, Entity entity, Operation<Boolean> original) {
        boolean spawned = original.call(world, entity);
        ActiveCooldowns.arrowSpawned(entity, spawned);
        return spawned;
    }
}
