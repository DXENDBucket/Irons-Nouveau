package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.entity.spells.ArrowVolleyEntity;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = ArrowVolleyEntity.class, remap = false)
public abstract class ArrowEmissionMixin {
    @WrapMethod(method = "tick", remap = true)
    private void ironsNouveau$emit(Operation<Void> original) {
        var entity = (ArrowVolleyEntity)(Object)this;
        if (!entity.level().isClientSide && entity.tickCount % 5 == 0)
            EffectResources.emit(entity, 300, true, original::call);
        else original.call();
    }
}
