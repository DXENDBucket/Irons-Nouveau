package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.ComplexImpacts;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.fireball.MagicFireball;
import io.redspace.ironsspellbooks.entity.spells.fire_arrow.FireArrowProjectile;
import io.redspace.ironsspellbooks.entity.spells.magma_ball.FireBomb;
import io.redspace.ironsspellbooks.entity.spells.snowball.Snowball;
import io.redspace.ironsspellbooks.entity.spells.acid_orb.AcidOrb;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = {MagicFireball.class, FireArrowProjectile.class, FireBomb.class, Snowball.class, AcidOrb.class,
        io.redspace.ironsspellbooks.entity.spells.WitherSkullProjectile.class,
        io.redspace.ironsspellbooks.entity.spells.creeper_head.CreeperHeadProjectile.class}, remap = false)
public abstract class ComplexImpactMixin {
    @WrapMethod(method = "onHit", remap = true)
    private void ironsNouveau$impact(HitResult hit, Operation<Void> original) {
        ComplexImpacts.execute((AbstractMagicProjectile)(Object)this, hit, () -> original.call(hit));
    }
}
