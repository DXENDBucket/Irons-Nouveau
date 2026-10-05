package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.items.data.ScryCasterData;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.ActiveCooldowns;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = ScryCasterData.class, remap = false)
public abstract class ScryCooldownMixin {
    @WrapOperation(method = "castSpell", at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;expendMana()V"))
    private void ironsNouveau$released(SpellResolver resolver, Operation<Void> original) {
        original.call(resolver);
        ActiveCooldowns.dispatched(resolver, true);
    }
}
