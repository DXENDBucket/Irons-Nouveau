package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Pay Ars' remaining carrier cost before branches spend mana; keeps Ars 'n' Spells' own payment route. */
@Mixin(value = SpellResolver.class, remap = false)
public abstract class ArsUpfrontPaymentMixin {
    @Unique private boolean ironsNouveau$paid;
    @Inject(method = {"onCast", "onCastOnBlock", "onCastOnEntity"}, at = {
        @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/api/spell/AbstractCastMethod;onCast(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/Level;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;)Lcom/hollingsworth/arsnouveau/api/spell/CastResolveType;"),
        @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/api/spell/AbstractCastMethod;onCastOnBlock(Lnet/minecraft/world/phys/BlockHitResult;Lnet/minecraft/world/entity/LivingEntity;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;)Lcom/hollingsworth/arsnouveau/api/spell/CastResolveType;"),
        @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/api/spell/AbstractCastMethod;onCastOnBlock(Lnet/minecraft/world/item/context/UseOnContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;)Lcom/hollingsworth/arsnouveau/api/spell/CastResolveType;"),
        @At(value = "INVOKE", target = "Lcom/hollingsworth/arsnouveau/api/spell/AbstractCastMethod;onCastOnEntity(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/InteractionHand;Lcom/hollingsworth/arsnouveau/api/spell/SpellStats;Lcom/hollingsworth/arsnouveau/api/spell/SpellContext;Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;)Lcom/hollingsworth/arsnouveau/api/spell/CastResolveType;")})
    private void ironsNouveau$beforeDispatch(CallbackInfoReturnable<Boolean> ci) {
        var resolver = (SpellResolver)(Object)this;
        if (!resolver.spellContext.getUnwrappedCaster().level().isClientSide
                && resolver.spell.unsafeList().stream().anyMatch(p -> p.getRegistryName().getNamespace().equals("irons_nouveau"))) {
            if (!(resolver.spellContext.getUnwrappedCaster() instanceof net.minecraft.world.entity.player.Player player) || !player.isCreative())
                resolver.expendMana();
            ironsNouveau$paid = true;
            dev.ironsnouveau.casting.ProjectileVolley.begin(resolver);
        }
    }
    @WrapOperation(method = {"onCast", "onCastOnBlock", "onCastOnEntity"}, at = @At(value = "INVOKE",
            target = "Lcom/hollingsworth/arsnouveau/api/spell/SpellResolver;expendMana()V"))
    private void ironsNouveau$once(SpellResolver resolver, Operation<Void> original) {
        if (!ironsNouveau$paid) original.call(resolver);
    }
    @Inject(method = {"onCast", "onCastOnBlock", "onCastOnEntity"}, at = @At("RETURN"))
    private void ironsNouveau$reset(CallbackInfoReturnable<Boolean> ci) {
        dev.ironsnouveau.casting.ProjectileVolley.finish((SpellResolver)(Object)this);
        ironsNouveau$paid = false;
    }
}
