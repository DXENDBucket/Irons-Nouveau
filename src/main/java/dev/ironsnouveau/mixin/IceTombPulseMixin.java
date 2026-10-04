package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = IceTombEntity.class, remap = false)
public abstract class IceTombPulseMixin {
    @WrapMethod(method = "doPositiveEffects")
    private void ironsNouveau$heal(Entity passenger, Operation<Void> original) {
        EffectResources.pulse((IceTombEntity)(Object)this, () -> { original.call(passenger); return true; });
    }
}
