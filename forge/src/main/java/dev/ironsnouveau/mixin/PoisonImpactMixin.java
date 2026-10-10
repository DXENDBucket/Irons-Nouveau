package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.ComplexImpacts;
import io.redspace.ironsspellbooks.entity.spells.poison_arrow.PoisonArrow;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = PoisonArrow.class, remap = false)
public abstract class PoisonImpactMixin {
    @WrapMethod(method = "onHitEntity", remap = true)
    private void ironsNouveau$entity(EntityHitResult hit, Operation<Void> original) { ComplexImpacts.execute((PoisonArrow)(Object)this, hit, () -> original.call(hit)); }
    @WrapMethod(method = "onHitBlock", remap = true)
    private void ironsNouveau$block(BlockHitResult hit, Operation<Void> original) { ComplexImpacts.execute((PoisonArrow)(Object)this, hit, () -> original.call(hit)); }
}
