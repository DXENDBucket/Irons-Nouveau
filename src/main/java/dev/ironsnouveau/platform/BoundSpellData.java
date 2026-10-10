package dev.ironsnouveau.platform;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.ironsnouveau.casting.BoundSpellWeapons;
import dev.ironsnouveau.casting.BoundSpellWeapons.Binding;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.RegistryAccess;
import net.neoforged.bus.api.IEventBus;

/** Native persistence and event wiring for the shared sealed-tool implementation. */
public final class BoundSpellData {
    private BoundSpellData() {}
    private static final net.neoforged.neoforge.registries.DeferredRegister<net.minecraft.core.component.DataComponentType<?>> COMPONENTS =
            net.neoforged.neoforge.registries.DeferredRegister.create(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, "irons_nouveau");
    private static final java.util.function.Supplier<net.minecraft.core.component.DataComponentType<Binding>> TYPE =
            COMPONENTS.register("bound_weapon_spell", () -> net.minecraft.core.component.DataComponentType.<Binding>builder()
                    .persistent(Binding.CODEC).networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.fromCodecWithRegistries(Binding.CODEC)).build());
    public static java.util.function.Supplier<net.minecraft.core.component.DataComponentType<Binding>> bindingType() { return TYPE; }
    public static boolean has(ItemStack stack) { return stack.has(TYPE.get()); }
    public static Binding get(ItemStack stack) { return stack.get(TYPE.get()); }
    public static void set(ItemStack stack, Binding binding) { stack.set(TYPE.get(), binding); }
    public static void clear(ItemStack stack) { stack.remove(TYPE.get()); }
    public static Spell copy(Spell spell) {
        return new Spell(spell.name(), spell.color(), spell.sound(),
                dev.arsconflux.api.glyph.ArsSpellAccess.parts(spell), spell.particleTimeline());
    }
    public static String name(Spell spell) { return spell.name(); }
    public static Spell fromBook(ItemStack stack) {
        return com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.hasCaster(stack)
                ? com.hollingsworth.arsnouveau.api.registry.SpellCasterRegistry.from(stack).getSpell() : null;
    }
    public static CompoundTag saveItem(ItemStack stack, RegistryAccess registries) {
        return (CompoundTag) stack.save(registries);
    }
    public static ItemStack loadItem(CompoundTag tag, RegistryAccess registries) {
        return ItemStack.parseOptional(registries, tag);
    }
    public static void register(IEventBus bus) {
        COMPONENTS.register(bus);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGHEST, BoundSpellData::rightClick);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(BoundSpellData::commands);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(BoundSpellData::tooltip);
    }
    private static void rightClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (!has(event.getItemStack())) return;
        var player = event.getEntity();
        boolean success = player.level().isClientSide || BoundSpellWeapons.cast(player, event.getItemStack(), event.getHand());
        event.setCancellationResult(success ? net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide)
                : net.minecraft.world.InteractionResult.FAIL);
        event.setCanceled(true);
    }
    private static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        BoundSpellWeapons.registerCommands(event.getDispatcher());
    }
    private static void tooltip(net.neoforged.neoforge.event.entity.player.ItemTooltipEvent event) {
        BoundSpellWeapons.appendTooltip(event.getItemStack(), event.getToolTip());
    }
}
