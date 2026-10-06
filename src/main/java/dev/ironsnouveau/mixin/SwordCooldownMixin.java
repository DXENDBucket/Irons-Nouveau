package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.items.EnchantersSword;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.ActiveCooldowns;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = EnchantersSword.class, remap = false)
public abstract class SwordCooldownMixin {
    @WrapOperation(method = "hurtEnemy", remap = true, at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;onCastOnEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;)Z", remap = false))
    private boolean ironsNouveau$magicOnly(SpellResolver resolver, ItemStack stack, Entity target, InteractionHand hand, Operation<Boolean> original) {
        return ActiveCooldowns.execute(resolver.spell, resolver.spellContext.getUnwrappedCaster(), CastSource.SWORD,
                () -> original.call(resolver, stack, target, hand), false);
    }
}
