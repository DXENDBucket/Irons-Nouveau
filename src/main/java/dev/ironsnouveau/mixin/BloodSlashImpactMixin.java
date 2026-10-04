package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.NativeCastCarrier;
import io.redspace.ironsspellbooks.entity.spells.blood_slash.BloodSlashProjectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value = BloodSlashProjectile.class, remap = false)
public abstract class BloodSlashImpactMixin {
    @WrapOperation(method = "damageEntity", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/damage/DamageSources;applyDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean ironsNouveau$hit(Entity target, float damage, DamageSource source, Operation<Boolean> original) {
        var session = ((NativeCastCarrier)this).ironsNouveau$session();
        if (session != null && !session.permitted()) return false;
        boolean applied = original.call(target, damage, source);
        if (applied && session != null) session.impact(new EntityHitResult(target, ((Entity)(Object)this).position()));
        return applied;
    }
}
