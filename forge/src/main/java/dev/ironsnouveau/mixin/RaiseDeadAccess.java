package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.spells.blood.RaiseDeadSpell;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.util.RandomSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = RaiseDeadSpell.class, remap = false)
public interface RaiseDeadAccess {
    @Invoker("getEquipment") ItemStack[] ironsNouveau$equipment(float power, RandomSource random);
    @Invoker("equip") void ironsNouveau$equip(Mob mob, ItemStack[] equipment);
}
