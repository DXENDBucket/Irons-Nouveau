package dev.ironsnouveau.progression;

import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ironsnouveau.IronsNouveau;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Covers loot tables and random glyph loot providers; native Iron scrolls remain legitimate loot. */
public final class NoNaturalGlyphs extends LootModifier {
    private static final MapCodec<NoNaturalGlyphs> CODEC = RecordCodecBuilder.mapCodec(instance ->
            codecStart(instance).apply(instance, NoNaturalGlyphs::new));
    public NoNaturalGlyphs(LootItemCondition[] conditions) { super(conditions); }
    public static void register(IEventBus bus) {
        var serializers = DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, IronsNouveau.MOD_ID);
        serializers.register("no_natural_glyphs", () -> CODEC); serializers.register(bus);
    }
    public static boolean isIronGlyph(ItemStack stack) {
        return stack.getItem() instanceof Glyph glyph && SpellProgress.spellId(glyph.spellPart) != null;
    }
    @Override protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (dev.ironsnouveau.config.SpellLevelConfig.requiresCrafting()) loot.removeIf(NoNaturalGlyphs::isIronGlyph);
        return loot;
    }
    @Override public MapCodec<? extends IGlobalLootModifier> codec() { return CODEC; }
}
