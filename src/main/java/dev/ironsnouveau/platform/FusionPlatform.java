package dev.ironsnouveau.platform;

import com.hollingsworth.arsnouveau.api.util.PerkUtil;
import dev.ironsnouveau.armor.ArmorFusion;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class FusionPlatform {
    private static final DeferredRegister<DataComponentType<?>> TYPES=DeferredRegister.create(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE,"irons_nouveau");
    // ItemStack itself is mutable and cannot be a component value. Vanilla's
    // immutable snapshot supplies stack-aware equality, hashing and both codecs.
    private static final java.util.function.Supplier<DataComponentType<ItemContainerContents>> TYPE=TYPES.register("armor_fusion",()->DataComponentType.<ItemContainerContents>builder()
            .persistent(ItemContainerContents.CODEC).networkSynchronized(ItemContainerContents.STREAM_CODEC).build());
    private FusionPlatform() {}
    public static boolean externalOwner(){return net.neoforged.fml.ModList.get().isLoaded("ars_fusion");}
    public static boolean has(ItemStack stack){return stack.has(TYPE.get());}
    public static ItemStack donor(ItemStack stack){return stack.getOrDefault(TYPE.get(),ItemContainerContents.EMPTY).copyOne();}
    public static void set(ItemStack stack,ItemStack donor){stack.set(TYPE.get(),ItemContainerContents.fromItems(java.util.List.of(donor)));}
    public static boolean customName(ItemStack stack){return stack.has(DataComponents.CUSTOM_NAME);}
    public static int tier(ItemStack stack){var perks=PerkUtil.getPerkHolder(stack);return perks==null?0:perks.getTier()+1;}
    public static void register(IEventBus bus) {
        TYPES.register(bus);NeoForge.EVENT_BUS.addListener(FusionPlatform::attributes);
    }
    private static void attributes(ItemAttributeModifierEvent event) {
        if(externalOwner())return;
        var stack=event.getItemStack();if(!ArmorFusion.ars(stack))return;
        var slot=ArmorFusion.slot(stack);var group=EquipmentSlotGroup.bySlot(slot);var donor=ArmorFusion.donor(stack);
        if(!donor.isEmpty())donor.forEachModifier(slot,(attribute,modifier)->{
            if(attribute.value()==Attributes.ARMOR.value()||attribute.value()==Attributes.ARMOR_TOUGHNESS.value())return;
            var key=BuiltInRegistries.ATTRIBUTE.getKey(attribute.value());var original=modifier.id();
            var id=ResourceLocation.fromNamespaceAndPath("irons_nouveau","fusion/"+slot.getName()+"/"+key.getNamespace()+"/"+key.getPath()+"/"+original.getNamespace()+"/"+original.getPath());
            event.addModifier(attribute,new AttributeModifier(id,modifier.amount(),modifier.operation()),group);
        });
        double toughness=ArmorFusion.toughness(stack);
        if(toughness!=0)event.addModifier(Attributes.ARMOR_TOUGHNESS,new AttributeModifier(ResourceLocation.fromNamespaceAndPath("irons_nouveau","ars_toughness/"+slot.getName()),toughness,AttributeModifier.Operation.ADD_VALUE),group);
    }
}
