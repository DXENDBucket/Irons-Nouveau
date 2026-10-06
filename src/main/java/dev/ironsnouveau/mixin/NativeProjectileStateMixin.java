package dev.ironsnouveau.mixin;

import dev.ironsnouveau.casting.CastSession;
import dev.ironsnouveau.casting.NativeCastCarrier;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

@Mixin(value = AbstractMagicProjectile.class, remap = false)
public abstract class NativeProjectileStateMixin implements NativeCastCarrier {
    @Unique private CastSession ironsNouveau$session;
    @Unique private static final EntityDataAccessor<Boolean> ironsNouveau$ARS_DRIVEN =
            SynchedEntityData.defineId(AbstractMagicProjectile.class, EntityDataSerializers.BOOLEAN);
    @Inject(method = "defineSynchedData", remap = true, at = @At("TAIL"))
    private void ironsNouveau$define(CallbackInfo ci) { ((AbstractMagicProjectile)(Object)this).getEntityData().define(ironsNouveau$ARS_DRIVEN, false); }
    @Override public boolean ironsNouveau$arsDriven() { return ((AbstractMagicProjectile)(Object)this).getEntityData().get(ironsNouveau$ARS_DRIVEN); }
    @Override public void ironsNouveau$arsDriven(boolean value) { ((AbstractMagicProjectile)(Object)this).getEntityData().set(ironsNouveau$ARS_DRIVEN, value); }
    @Inject(method = "tick", remap = true, at = @At("HEAD"), cancellable = true)
    private void ironsNouveau$visualOnly(CallbackInfo ci) {
        if (!ironsNouveau$arsDriven()) return;
        var projectile = (AbstractMagicProjectile)(Object)this;
        if (projectile.level().isClientSide) {
            projectile.setPos(projectile.position().add(projectile.getDeltaMovement()));
            projectile.trailParticles();
        }
        ci.cancel();
    }
    @Override public CastSession ironsNouveau$session() { return ironsNouveau$session; }
    @Override public void ironsNouveau$session(CastSession session) { ironsNouveau$session = session; }
    @Inject(method = "shouldBeSaved", remap = true, at = @At("HEAD"), cancellable = true)
    private void ironsNouveau$ephemeral(CallbackInfoReturnable<Boolean> cir) {
        if (ironsNouveau$session != null || ironsNouveau$arsDriven()) cir.setReturnValue(false);
    }
}
