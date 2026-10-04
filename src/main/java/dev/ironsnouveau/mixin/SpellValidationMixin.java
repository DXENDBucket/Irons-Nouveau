package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellValidationError;
import com.hollingsworth.arsnouveau.common.spell.validation.StandardSpellValidator;
import dev.ironsnouveau.casting.NativeCasting;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

@Mixin(value = StandardSpellValidator.class, remap = false)
public abstract class SpellValidationMixin {
    @Inject(method = "validate", at = @At("RETURN"), cancellable = true)
    private void ironsNouveau$validate(List<AbstractSpellPart> recipe, CallbackInfoReturnable<List<SpellValidationError>> cir) {
        var errors = NativeCasting.validate(recipe);
        if (!errors.isEmpty()) {
            var combined = new ArrayList<>(cir.getReturnValue());
            combined.addAll(errors);
            cir.setReturnValue(combined);
        }
    }
}
