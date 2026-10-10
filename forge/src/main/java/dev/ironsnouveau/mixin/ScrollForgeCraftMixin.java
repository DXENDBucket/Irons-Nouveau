package dev.ironsnouveau.mixin;
import dev.ironsnouveau.progression.SpellProgress;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ScrollForgeMenu.class, remap = false)
public abstract class ScrollForgeCraftMixin {
    // Iron passes an emptied stack to onTake after shift-moving it. Its return retains the actual crafted result.
    @Inject(method = "quickMoveStack", remap = true, at = @At("RETURN"))
    private void ironsNouveau$shiftCraft(Player player, int slot, CallbackInfoReturnable<ItemStack> cir) {
        if (slot == ((ScrollForgeMenu)(Object)this).slots.indexOf(((ScrollForgeMenu)(Object)this).getResultSlot()))
            SpellProgress.crafted(player, cir.getReturnValue());
    }
}
