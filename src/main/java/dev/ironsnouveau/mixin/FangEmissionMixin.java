package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.entity.spells.FangSwirlEntity;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = FangSwirlEntity.class, remap = false)
public abstract class FangEmissionMixin {
    @WrapMethod(method = "checkHits")
    private void ironsNouveau$emit(Operation<Void> original) {
        EffectResources.emit((FangSwirlEntity)(Object)this, 100, true, original::call);
    }
}
