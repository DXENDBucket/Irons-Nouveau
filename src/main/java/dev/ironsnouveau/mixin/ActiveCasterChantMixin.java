package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.api.spell.AbstractCaster;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.items.data.ScryCasterData;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.ActiveChanting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = {AbstractCaster.class, ScryCasterData.class}, remap = false)
public abstract class ActiveCasterChantMixin {
    @WrapMethod(method = "castSpell(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/network/chat/Component;Lcom/hollingsworth/arsnouveau/api/spell/Spell;)Lnet/minecraft/world/InteractionResultHolder;")
    private InteractionResultHolder<ItemStack> ironsNouveau$chant(Level world, LivingEntity caster, InteractionHand hand,
            Component invalidMessage, Spell spell, Operation<InteractionResultHolder<ItemStack>> original) {
        if (ActiveChanting.defer(spell, caster, hand, () -> original.call(world, caster, hand, invalidMessage, spell)))
            return InteractionResultHolder.consume(caster.getItemInHand(hand));
        return original.call(world, caster, hand, invalidMessage, spell);
    }
}
