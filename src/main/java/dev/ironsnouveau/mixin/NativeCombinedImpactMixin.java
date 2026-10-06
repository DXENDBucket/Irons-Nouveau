package dev.ironsnouveau.mixin;

import dev.ironsnouveau.casting.NativeCastCarrier;
import io.redspace.ironsspellbooks.entity.spells.lightning_lance.LightningLanceProjectile;
import io.redspace.ironsspellbooks.entity.spells.blood_needle.BloodNeedle;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** These native projectiles handle removal in onHit and do not both override onHitBlock. */
@Mixin(value = {LightningLanceProjectile.class, BloodNeedle.class}, remap = false)
public abstract class NativeCombinedImpactMixin {
    @Inject(method = "onHit", remap = true, at = @At("TAIL"))
    private void ironsNouveau$block(HitResult hit, CallbackInfo ci) {
        var session = ((NativeCastCarrier)this).ironsNouveau$session();
        if (session != null && hit.getType() == HitResult.Type.BLOCK) session.impact(hit);
    }
}
