package dev.ironsnouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.NativeCastCarrier;
import io.redspace.ironsspellbooks.entity.spells.ball_lightning.BallLightning;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = BallLightning.class, remap = false)
public abstract class BallLightningImpactMixin {
    @Unique private long ironsNouveau$paidUntil = Long.MIN_VALUE;
    @org.spongepowered.asm.mixin.injection.Inject(method = "onHitBlock", at = @At("HEAD"))
    private void ironsNouveau$block(net.minecraft.world.phys.BlockHitResult hit, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        var session = ((NativeCastCarrier)this).ironsNouveau$session();
        // Resolve with the incoming direction, before the native bounce reverses velocity.
        if (session != null) session.impact(hit);
    }
    @WrapOperation(method = "onHitEntity", at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/damage/DamageSources;applyDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean ironsNouveau$hit(Entity target, float damage, DamageSource source, Operation<Boolean> original) {
        var session = ((NativeCastCarrier)this).ironsNouveau$session();
        if (session == null) return original.call(target, damage, source);
        if (!session.permitted()) return false;
        long now = session.world().getGameTime();
        java.util.function.BooleanSupplier hit = () -> original.call(target, damage, source);
        boolean paid = ironsNouveau$paidUntil == Long.MIN_VALUE || now < ironsNouveau$paidUntil;
        boolean applied = paid || session.billingSource() == null ? hit.getAsBoolean()
                : session.billingSource().trigger(session.plan().spellId(), session.plan().spellLevel(), hit);
        if (applied) {
            if (!paid || ironsNouveau$paidUntil == Long.MIN_VALUE) ironsNouveau$paidUntil = now + 10;
            session.impact(new EntityHitResult(target, ((Entity)(Object)this).position()));
        }
        return applied;
    }
}
