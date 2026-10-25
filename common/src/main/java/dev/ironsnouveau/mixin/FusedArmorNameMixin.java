package dev.ironsnouveau.mixin;

import dev.ironsnouveau.armor.ArmorFusion;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public abstract class FusedArmorNameMixin {
    @Inject(method="getHoverName",at=@At("RETURN"),cancellable=true)
    private void ironsNouveau$fusionName(CallbackInfoReturnable<Component> cir) {
        cir.setReturnValue(ArmorFusion.name((ItemStack)(Object)this,cir.getReturnValue()));
    }
}
