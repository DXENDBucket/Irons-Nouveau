package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.entity.spells.BlizzardAoe;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = BlizzardAoe.class, remap = false)
public abstract class BlizzardPulseMixin {
    @WrapMethod(method = "tick", remap = true)
    private void ironsNouveau$blizzard(Operation<Void> original) {
        EffectResources.pulse((BlizzardAoe)(Object)this, 10, () -> { original.call(); return true; });
    }
}
