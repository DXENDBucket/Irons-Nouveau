package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.ActiveChanting;
import dev.ironsnouveau.casting.ActiveCooldowns;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = com.hollingsworth.arsnouveau.api.spell.SpellCaster.class, remap = false)
public abstract class ActiveCasterChantMixin {
    @WrapMethod(method = "castSpell(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/network/chat/Component;Lcom/hollingsworth/arsnouveau/api/spell/Spell;)Lnet/minecraft/world/InteractionResultHolder;")
    private InteractionResultHolder<ItemStack> ironsNouveau$chant(Level world, LivingEntity caster, InteractionHand hand,
            Component invalidMessage, Spell spell, Operation<InteractionResultHolder<ItemStack>> original) {
        if (dev.arsconflux.api.interaction.InputSessions.route(caster, hand, spell))
            return InteractionResultHolder.consume(caster.getItemInHand(hand));
        if (!ActiveCooldowns.allowed(spell, caster)) return InteractionResultHolder.fail(caster.getItemInHand(hand));
        java.util.function.Supplier<InteractionResultHolder<ItemStack>> release = () -> ActiveCooldowns.execute(spell, caster,
                CastSource.SPELLBOOK, () -> original.call(world, caster, hand, invalidMessage, spell),
                InteractionResultHolder.fail(caster.getItemInHand(hand)));
        if (ActiveChanting.defer(spell, caster, hand, () -> release.get()))
            return InteractionResultHolder.consume(caster.getItemInHand(hand));
        return release.get();
    }
}
