package dev.ironsnouveau.mixin;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.ironsnouveau.casting.TriggerGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = EntityProjectileSpell.class, remap = false)
public abstract class ArsTriggerGeometryMixin {
    @Inject(method = "resolver", at = @At("RETURN"))
    private void ironsNouveau$direction(CallbackInfoReturnable<SpellResolver> ci) {
        var projectile = (EntityProjectileSpell)(Object)this;
        if (!projectile.level().isClientSide && ci.getReturnValue() != null)
            TriggerGeometry.write(ci.getReturnValue().spellContext, TriggerGeometry.direction(projectile));
    }
}
