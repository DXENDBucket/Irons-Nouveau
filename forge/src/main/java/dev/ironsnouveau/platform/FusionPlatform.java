package dev.ironsnouveau.platform;

import com.hollingsworth.arsnouveau.api.util.PerkUtil;
import dev.ironsnouveau.armor.ArmorFusion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AnvilUpdateEvent;
import net.minecraftforge.event.ItemAttributeModifierEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class FusionPlatform {
    private static final String KEY="irons_nouveau:armor_fusion";
    private FusionPlatform() {}
    public static boolean has(ItemStack stack){return stack.hasTag()&&stack.getTag().contains(KEY,net.minecraft.nbt.Tag.TAG_COMPOUND);}
    public static ItemStack donor(ItemStack stack){return has(stack)?ItemStack.of(stack.getTag().getCompound(KEY)):ItemStack.EMPTY;}
    public static void set(ItemStack stack,ItemStack donor){stack.getOrCreateTag().put(KEY,donor.save(new net.minecraft.nbt.CompoundTag()));}
    public static boolean customName(ItemStack stack){return stack.hasCustomHoverName();}
    public static int tier(ItemStack stack){var perks=PerkUtil.getPerkHolder(stack);return perks==null?0:perks.getTier()+1;}
    public static void register(IEventBus bus) {MinecraftForge.EVENT_BUS.addListener(FusionPlatform::anvil);MinecraftForge.EVENT_BUS.addListener(FusionPlatform::attributes);}
    private static void anvil(AnvilUpdateEvent event) {
        var output=ArmorFusion.fuse(event.getLeft(),event.getRight());if(output.isEmpty())return;
        String name=event.getName();
        if(name!=null&&!name.isBlank()&&!name.equals(event.getLeft().getHoverName().getString()))output.setHoverName(Component.literal(name));
        event.setOutput(output);event.setCost(1);event.setMaterialCost(1);
    }
    private static void attributes(ItemAttributeModifierEvent event) {
        var stack=event.getItemStack();if(!ArmorFusion.ars(stack)||event.getSlotType()!=ArmorFusion.slot(stack))return;
        var slot=ArmorFusion.slot(stack);var donor=ArmorFusion.donor(stack);
        if(!donor.isEmpty())donor.getAttributeModifiers(slot).forEach((attribute,modifier)->{
            if(attribute==Attributes.ARMOR||attribute==Attributes.ARMOR_TOUGHNESS)return;
            String id="irons_nouveau:armor_fusion/"+slot.getName()+"/"+BuiltInRegistries.ATTRIBUTE.getKey(attribute)+"/"+modifier.getId();
            event.addModifier(attribute,new AttributeModifier(UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)),id,modifier.getAmount(),modifier.getOperation()));
        });
        double toughness=ArmorFusion.toughness(stack);
        if(toughness!=0)event.addModifier(Attributes.ARMOR_TOUGHNESS,new AttributeModifier(UUID.nameUUIDFromBytes(("irons_nouveau:ars_toughness/"+slot.getName()).getBytes(StandardCharsets.UTF_8)),"Ars armor tier toughness",toughness,AttributeModifier.Operation.ADDITION));
    }
}
