package dev.ironsnouveau.mixin;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import dev.ironsnouveau.casting.ProjectileVolley;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = SpellStats.class, remap = false)
public abstract class ArsSplitBudgetMixin {
    @Inject(method = "getBuffCount", at = @At("RETURN"), cancellable = true)
    private void ironsNouveau$count(AbstractAugment augment, CallbackInfoReturnable<Integer> ci) {
        if (augment == AugmentSplit.INSTANCE) ci.setReturnValue(ProjectileVolley.splits((SpellStats)(Object)this, ci.getReturnValue()));
    }
}
