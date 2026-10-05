package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.ironsnouveau.casting.ActiveCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = SpellResolver.class, remap = false)
public abstract class ActiveCooldownDispatchMixin {
    @Inject(method = {"onCast", "onCastOnBlock", "onCastOnEntity"}, at = @At("HEAD"), cancellable = true)
    private void ironsNouveau$check(CallbackInfoReturnable<Boolean> ci) {
        if (!ActiveCooldowns.allowDispatch((SpellResolver)(Object)this)) ci.setReturnValue(false);
    }
    @Inject(method = {"onCast", "onCastOnBlock", "onCastOnEntity"}, at = @At("RETURN"))
    private void ironsNouveau$released(CallbackInfoReturnable<Boolean> ci) {
        ActiveCooldowns.dispatched((SpellResolver)(Object)this, ci.getReturnValueZ());
    }
}
