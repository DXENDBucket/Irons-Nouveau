package dev.ironsnouveau.mixin;

import dev.ironsnouveau.casting.NativeCastCarrier;
import io.redspace.ironsspellbooks.entity.spells.firebolt.FireboltProjectile;
import io.redspace.ironsspellbooks.entity.spells.icicle.IcicleProjectile;
import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import io.redspace.ironsspellbooks.entity.spells.guiding_bolt.GuidingBoltProjectile;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = {FireboltProjectile.class, IcicleProjectile.class, MagicMissileProjectile.class,
        GuidingBoltProjectile.class, MagicArrowProjectile.class,
        io.redspace.ironsspellbooks.entity.spells.blood_slash.BloodSlashProjectile.class,
        io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball.class}, remap = false)
public abstract class NativeBlockImpactMixin {
    @Inject(method = "onHitBlock", remap = true, at = @At("TAIL"))
    private void ironsNouveau$block(BlockHitResult hit, CallbackInfo ci) {
        var session = ((NativeCastCarrier)this).ironsNouveau$session();
        if (session != null) session.impact(hit);
    }
}
