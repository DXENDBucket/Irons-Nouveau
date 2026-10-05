package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.spells.evocation.FirecrackerSpell;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = FirecrackerSpell.class, remap = false)
public interface FirecrackerAccess {
    @Invoker("randomFireworkRocket") ItemStack ironsNouveau$rocket();
}
