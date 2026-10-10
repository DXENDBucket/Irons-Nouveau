package dev.ironsnouveau.platform;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.ironsnouveau.casting.BoundSpellWeapons;
import dev.ironsnouveau.casting.BoundSpellWeapons.Binding;
import net.minecraft.world.item.ItemStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.RegistryAccess;
import net.minecraftforge.eventbus.api.IEventBus;

/** Native persistence and event wiring for the shared sealed-tool implementation. */
public final class BoundSpellData {
    private BoundSpellData() {}
    private static final String KEY = "irons_nouveau:bound_weapon_spell";
    public static boolean has(ItemStack stack) { return stack.hasTag() && stack.getTag().contains(KEY, net.minecraft.nbt.Tag.TAG_COMPOUND); }
    public static Binding get(ItemStack stack) {
        if (!has(stack)) return null;
        var tag = stack.getTag().getCompound(KEY);
        if (!tag.contains("spell", net.minecraft.nbt.Tag.TAG_COMPOUND)) return null;
        var levels = new java.util.HashMap<net.minecraft.resources.ResourceLocation, Integer>();
        var saved = tag.getCompound("iron_levels");
        for (var key : saved.getAllKeys()) {
            var id = net.minecraft.resources.ResourceLocation.tryParse(key);
            int level = saved.getInt(key);
            if (id != null && level > 0) levels.put(id, level);
        }
        return new Binding(Spell.fromTag(tag.getCompound("spell")), levels);
    }
    public static void set(ItemStack stack, Binding binding) {
        var tag = new CompoundTag();
        tag.put("spell", binding.spell().serialize());
        var levels = new CompoundTag();
        binding.ironLevels().forEach((id, level) -> levels.putInt(id.toString(), level));
        tag.put("iron_levels", levels);
        stack.getOrCreateTag().put(KEY, tag);
    }
    public static void clear(ItemStack stack) { if (stack.hasTag()) stack.getTag().remove(KEY); }
    public static Spell copy(Spell spell) { return spell.clone(); }
    public static String name(Spell spell) { return spell.name; }
    public static Spell fromBook(ItemStack stack) {
        return stack.getItem() instanceof com.hollingsworth.arsnouveau.common.items.SpellBook
                ? new com.hollingsworth.arsnouveau.api.spell.SpellCaster(stack).getSpell() : null;
    }
    public static CompoundTag saveItem(ItemStack stack, RegistryAccess registries) {
        return stack.save(new CompoundTag());
    }
    public static ItemStack loadItem(CompoundTag tag, RegistryAccess registries) {
        return ItemStack.of(tag);
    }
    public static void register(IEventBus bus) {
        // Forge stores this payload in synchronized ItemStack NBT.
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(net.minecraftforge.eventbus.api.EventPriority.HIGHEST, BoundSpellData::rightClick);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(BoundSpellData::commands);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(BoundSpellData::tooltip);
    }
    private static void rightClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickItem event) {
        if (!has(event.getItemStack())) return;
        var player = event.getEntity();
        boolean success = player.level().isClientSide || BoundSpellWeapons.cast(player, event.getItemStack(), event.getHand());
        event.setCancellationResult(success ? net.minecraft.world.InteractionResult.sidedSuccess(player.level().isClientSide)
                : net.minecraft.world.InteractionResult.FAIL);
        event.setCanceled(true);
    }
    private static void commands(net.minecraftforge.event.RegisterCommandsEvent event) {
        BoundSpellWeapons.registerCommands(event.getDispatcher());
    }
    private static void tooltip(net.minecraftforge.event.entity.player.ItemTooltipEvent event) {
        BoundSpellWeapons.appendTooltip(event.getItemStack(), event.getToolTip());
    }
}
