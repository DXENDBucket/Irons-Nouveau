package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.entity.spells.creeper_head.CreeperHeadProjectile;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value = CreeperHeadProjectile.class, remap = false)
public interface CreeperHeadAccess {
    @Accessor("speed") void ironsNouveau$setSpeed(float speed);
}
