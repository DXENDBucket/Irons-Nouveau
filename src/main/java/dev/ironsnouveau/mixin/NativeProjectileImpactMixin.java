package dev.ironsnouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.NativeCastCarrier;
import io.redspace.ironsspellbooks.entity.spells.firebolt.FireboltProjectile;
import io.redspace.ironsspellbooks.entity.spells.icicle.IcicleProjectile;
import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import io.redspace.ironsspellbooks.entity.spells.guiding_bolt.GuidingBoltProjectile;
import io.redspace.ironsspellbooks.entity.spells.lightning_lance.LightningLanceProjectile;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile;
import io.redspace.ironsspellbooks.entity.spells.blood_needle.BloodNeedle;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {FireboltProjectile.class, IcicleProjectile.class, MagicMissileProjectile.class,
        GuidingBoltProjectile.class, LightningLanceProjectile.class, MagicArrowProjectile.class, BloodNeedle.class}, remap = false)
public abstract class NativeProjectileImpactMixin {
    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/damage/DamageSources;applyDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean ironsNouveau$impact(Entity target, float damage, DamageSource source, Operation<Boolean> original) {
        var session = ((NativeCastCarrier) this).ironsNouveau$session();
        if (session != null && !session.permitted()) return false;
        boolean applied = original.call(target, damage, source);
        if (applied && session != null) session.impact(new EntityHitResult(target, ((Entity)(Object)this).position()));
        return applied;
    }
}
