package dev.ironsnouveau.mixin;
import dev.ironsnouveau.progression.SpellProgress;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The result slot commits ingredient consumption before awarding a normal click craft. */
@Mixin(targets = "io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu$4", remap = false)
public abstract class ScrollForgeResultMixin {
    @Inject(method = "onTake", remap = true, at = @At("RETURN"))
    private void ironsNouveau$crafted(Player player, ItemStack output, CallbackInfo ci) { SpellProgress.crafted(player, output); }
}
