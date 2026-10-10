package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.StepAimGeometry;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value = TeleportSpell.class, remap = false)
public abstract class StepLandingDirectionMixin {
    @WrapOperation(method = "solveTeleportDestination", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getForward()Lnet/minecraft/world/phys/Vec3;"))
    private static Vec3 ironsNouveau$forward(LivingEntity caster, Operation<Vec3> original) {
        return StepAimGeometry.forward(caster, original.call(caster));
    }
}
