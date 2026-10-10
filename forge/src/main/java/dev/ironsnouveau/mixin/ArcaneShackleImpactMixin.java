package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.NativeCastCarrier;
import io.redspace.ironsspellbooks.entity.spells.ender_chain.ArcaneShackleProjectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = ArcaneShackleProjectile.class, remap = false)
public abstract class ArcaneShackleImpactMixin {
    @WrapMethod(method = "onHitEntity", remap = true)
    private void ironsNouveau$entity(EntityHitResult hit, Operation<Void> original) {
        var session = ((NativeCastCarrier)this).ironsNouveau$session();
        if (session != null && !session.permitted()) return;
        original.call(hit);
        if (session != null) session.impact(hit);
    }
    @WrapMethod(method = "onHitBlock", remap = true)
    private void ironsNouveau$block(BlockHitResult hit, Operation<Void> original) {
        var session = ((NativeCastCarrier)this).ironsNouveau$session();
        if (session != null && !session.permitted()) return;
        original.call(hit);
        if (session != null) session.impact(hit);
    }
}
