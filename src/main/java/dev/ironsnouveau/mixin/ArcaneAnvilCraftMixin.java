package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.progression.SpellProgress;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = ArcaneAnvilMenu.class, remap = false)
public abstract class ArcaneAnvilCraftMixin {
    @WrapMethod(method = "onTake", remap = true)
    private void ironsNouveau$upgrade(Player player, ItemStack taken, Operation<Void> original) {
        var menu = (ArcaneAnvilMenu)(Object)this;
        var base = menu.getSlot(0).getItem(); var modifier = menu.getSlot(1).getItem();
        var result = ItemStack.EMPTY;
        // Capture before consumption. Quick-move may pass an emptied output stack here as well.
        if (base.getItem() instanceof Scroll && modifier.getItem() instanceof InkItem ink && ISpellContainer.get(base) != null) {
            var data = ISpellContainer.get(base).getSpellAtIndex(0);
            if (data.getLevel() < data.getSpell().getMaxLevel() && data.getSpell().getRarity(data.getLevel() + 1) == ink.getRarity()) {
                result = base.copyWithCount(1);
                ISpellContainer.createScrollContainer(data.getSpell(), data.getLevel() + 1, result);
            }
        }
        original.call(player, taken);
        SpellProgress.crafted(player, result);
    }
}
