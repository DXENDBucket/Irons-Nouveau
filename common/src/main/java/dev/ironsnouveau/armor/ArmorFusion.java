package dev.ironsnouveau.armor;

import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.platform.FusionPlatform;
import io.redspace.ironsspellbooks.api.spells.IPresetSpellContainer;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.armor.ExtendedArmorItem;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;

/** The output remains the original Ars item. Iron is a one-time donor, never a
 * replacement item identity, armor material, enchantment set or durability pool. */
public final class ArmorFusion {
    private ArmorFusion() {}
    public static boolean iron(ItemStack stack) {return stack.getItem() instanceof ExtendedArmorItem;}
    public static boolean ars(ItemStack stack) {
        return stack.getItem() instanceof ArmorItem&&!iron(stack)&&FusionPlatform.tier(stack)>0;
    }
    public static EquipmentSlot slot(ItemStack stack) {return ((ArmorItem)stack.getItem()).getType().getSlot();}
    public static boolean has(ItemStack stack) {return ars(stack)&&FusionPlatform.has(stack);}
    public static ItemStack donor(ItemStack stack) {
        if(!has(stack))return ItemStack.EMPTY;
        var donor=FusionPlatform.donor(stack);
        return iron(donor)&&slot(donor)==slot(stack)?donor:ItemStack.EMPTY;
    }
    public static boolean canFuse(ItemStack ars,ItemStack iron) {
        return SpellLevelConfig.armorFusionEnabled()&&ars(ars)&&iron(iron)&&!has(ars)&&slot(ars)==slot(iron);
    }
    public static ItemStack fuse(ItemStack ars,ItemStack iron) {
        if(!canFuse(ars,iron))return ItemStack.EMPTY;
        var donor=iron.copy();donor.setCount(1);
        if(donor.getItem() instanceof IPresetSpellContainer preset)preset.initializeSpellContainer(donor);
        var result=ars.copy();result.setCount(1);FusionPlatform.set(result,donor);
        var spells=ISpellContainer.get(donor);
        if(spells!=null)ISpellContainer.set(result,spells.mutableCopy().toImmutable());
        return result;
    }
    public static Component name(ItemStack stack,Component original) {
        var donor=donor(stack);
        return donor.isEmpty()||FusionPlatform.customName(stack)?original:
                Component.translatable("item.irons_nouveau.fused_armor",original,donor.getHoverName());
    }
    public static double toughness(ItemStack stack) {
        return ars(stack)?SpellLevelConfig.arsArmorToughnessPerTier()*FusionPlatform.tier(stack):0;
    }
}
