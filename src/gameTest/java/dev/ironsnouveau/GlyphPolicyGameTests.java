package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.ironsnouveau.progression.SpellProgress;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.neoforged.neoforge.gametest.*;
import java.util.stream.Stream;

@GameTestHolder("irons_nouveau_policy")
@PrefixGameTestTemplate(false)
public final class GlyphPolicyGameTests {
    private static java.util.List<AbstractSpellPart> glyphs() {
        return Stream.<AbstractSpellPart>concat(IronsNouveau.glyphs().stream(), IronsNouveau.forms().stream()).toList();
    }
    @GameTest(template = "empty")
    public static void tierOneOverridesLegacyConfiguration(GameTestHelper h) {
        h.assertTrue(glyphs().size() == 95, "Every registered Iron glyph is covered");
        for (var glyph : glyphs()) {
            int saved = glyph.GLYPH_TIER.get();
            try {
                glyph.GLYPH_TIER.set(3);
                h.assertTrue(glyph.defaultTier() == SpellTier.ONE && glyph.getConfigTier() == SpellTier.ONE,
                        "Old tier config cannot reintroduce a book lock: " + glyph.getRegistryName());
                h.assertTrue(!glyph.shouldShowInUnlock(), "No Ars learning-menu unlock");
            } finally { glyph.GLYPH_TIER.set(saved); }
        }
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void creativeUsesArsKnowledgeAndSurvivalRequiresCrafting(GameTestHelper h) {
        var player = ScrollProgressGameTests.player(h);
        var cap = CapabilityRegistry.getPlayerDataCap(player);
        for (var glyph : glyphs()) {
            h.assertTrue(!cap.unlockGlyph(glyph) && !cap.knowsGlyph(glyph), "Direct glyph learning cannot bypass crafting");
        }
        player.setGameMode(GameType.CREATIVE);
        h.assertTrue(cap.getKnownGlyphs().stream().noneMatch(g -> SpellProgress.spellId(g) != null), "Creative ordinary books still read actual learned glyphs");
        for (var glyph : glyphs()) {
            var id = SpellProgress.spellId(glyph);
            h.assertTrue(!cap.knowsGlyph(glyph), "Creative mode alone does not teach a glyph");
            h.assertTrue(SpellProgress.baseLevel(player, id) == SpellRegistry.getSpell(id).getMaxLevel(), "Creative uses native upgrade maximum");
        }
        var learned = glyphs().getFirst();
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(learned.getGlyph()));
        learned.getGlyph().use(h.getLevel(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
        h.assertTrue(cap.knowsGlyph(learned) && cap.getKnownGlyphs().contains(learned), "Creative glyph use learns through Ars normally");
        h.assertTrue(player.getMainHandItem().getCount() == 1 && SpellProgress.craftedLevel(player, SpellProgress.spellId(learned)) == 0,
                "Creative learning neither consumes the glyph nor creates scroll history");
        player.setGameMode(GameType.SURVIVAL);
        h.assertTrue(cap.getKnownGlyphs().stream().noneMatch(g -> SpellProgress.spellId(g) != null), "Creative grants no survival knowledge");
        player.setGameMode(GameType.CREATIVE);
        h.assertTrue(cap.knowsGlyph(learned), "Ars learned record persists across mode switches");
        player.setGameMode(GameType.SURVIVAL);
        var menu = ScrollProgressGameTests.forge(h, player, SpellRarity.COMMON);
        menu.clicked(39, 0, ClickType.PICKUP, player);
        var id = SpellRegistry.FIREBOLT_SPELL.get().getSpellResource();
        var firebolt = glyphs().stream().filter(g -> id.equals(SpellProgress.spellId(g))).findFirst().orElseThrow();
        h.assertTrue(cap.knowsGlyph(firebolt) && cap.getKnownGlyphs().contains(firebolt), "A real completed scroll craft unlocks the glyph");
        player.setGameMode(GameType.CREATIVE); cap.getKnownGlyphs(); player.setGameMode(GameType.SURVIVAL);
        h.assertTrue(SpellProgress.baseLevel(player, id) == 1 && cap.knowsGlyph(firebolt), "Mode switching preserves earned base and knowledge");
        player.discard(); h.succeed();
    }
    @GameTest(template = "empty")
    public static void naturalLootRemovesOnlyIronGlyphs(GameTestHelper h) {
        var key = ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.fromNamespaceAndPath("irons_nouveau_policy", "glyph_probe"));
        var table = h.getLevel().getServer().reloadableRegistries().getLootTable(key);
        var loot = table.getRandomItems(new LootParams.Builder(h.getLevel()).create(LootContextParamSets.EMPTY));
        h.assertTrue(loot.size() == 2, "Global loot filter removes the Iron glyph from the three-item fixture");
        h.assertTrue(loot.stream().anyMatch(s -> s.is(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get())), "Native scroll loot remains");
        h.assertTrue(loot.stream().anyMatch(s -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString().equals("ars_nouveau:glyph_light")), "Ars glyph loot remains");
        h.succeed();
    }
}
