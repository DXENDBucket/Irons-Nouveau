package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.entity.spells.black_hole.BlackHole;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = BlackHole.class, remap = false)
public abstract class BlackHolePulseMixin {
    @WrapMethod(method = "tick")
    private void ironsNouveau$gravity(Operation<Void> original) {
        EffectResources.pulse((BlackHole)(Object)this, 10, () -> { original.call(); return true; });
    }
}
