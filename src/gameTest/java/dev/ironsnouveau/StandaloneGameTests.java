package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.block.ScribesBlock;
import com.hollingsworth.arsnouveau.common.block.ThreePartBlock;
import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.progression.SpellProgress;
import dev.ironsnouveau.recipe.SpellScrollIngredient;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.*;
import java.util.ArrayList;
import java.util.List;

@GameTestHolder("irons_nouveau_standalone")
@PrefixGameTestTemplate(false)
public final class StandaloneGameTests {
    private static void close(GameTestHelper h, double actual, double expected, String message) {
        h.assertTrue(Math.abs(actual - expected) < .0001, message + ": " + actual + " != " + expected);
    }
    private static ItemStack scroll(String id, int level) {
        var stack = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(SpellRegistry.getSpell(ResourceLocation.parse("irons_spellbooks:" + id)), level, stack);
        return stack;
    }
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void independentRuntimeSupportsBothLevelAndLearningPolicies(GameTestHelper h) {
        for (var mod : List.of("ars_n_spells", "not_enough_glyphs", "path_to_zero", "jei"))
            h.assertTrue(!ModList.get().isLoaded(mod), "Standalone test must exclude " + mod);
        h.assertTrue(IronsNouveau.forms().size() == 18 && IronsNouveau.glyphs().size() == 77, "All 95 glyphs register independently");
        var player = ScrollProgressGameTests.player(h);
        var cap = CapabilityRegistry.getPlayerDataCap(player);
        var fire = SpellRegistry.FIREBOLT_SPELL.get(); var id = fire.getSpellResource();
        var form = IronsNouveau.forms().stream().filter(g -> g.spellId().equals(id)).findFirst().orElseThrow();
        var heal = IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals("heal")).findFirst().orElseThrow();
        var previousMode = SpellLevelConfig.MODE.get(); var previousDefault = SpellLevelConfig.DEFAULT_LEVEL.get();
        var previousOverrides = SpellLevelConfig.SPELL_LEVELS.get(); var previousCreative = SpellLevelConfig.CREATIVE_NATIVE_MAX.get();
        var previousLearning = SpellLevelConfig.LEARNING_MODE.get();
        try {
            h.assertTrue(SpellLevelConfig.SPEC.isLoaded() && previousMode == SpellLevelConfig.Mode.PERSONAL_SCROLL
                    && previousLearning == SpellLevelConfig.LearningMode.SCROLL_CRAFTING, "Loaded server config preserves existing defaults");
            h.assertTrue(!cap.knowsGlyph(form) && !cap.unlockGlyph(form), "Default learning still requires personal crafting");
            var menu = ScrollProgressGameTests.forge(h, player, SpellRarity.COMMON);
            menu.clicked(39, 0, ClickType.PICKUP, player);
            h.assertTrue(cap.knowsGlyph(form) && SpellProgress.craftedLevel(player, id) == 1, "Real native crafting works without Ars n Spells");
            SpellLevelConfig.MODE.set(SpellLevelConfig.Mode.FIXED);
            SpellLevelConfig.DEFAULT_LEVEL.set(2);
            SpellLevelConfig.SPELL_LEVELS.set(List.of("irons_spellbooks:firebolt=3", "irons_spellbooks:heal=4"));
            close(h, SpellProgress.baseLevel(player, id), 3, "Per-spell override");
            close(h, SpellProgress.baseLevel(player, SpellRegistry.ICICLE_SPELL.get().getSpellResource()), 2, "Fallback level");
            close(h, SpellLevels.resolve(player, id, List.of(AugmentAmplify.INSTANCE)), 4, "Amplification applied after fixed base");
            h.assertTrue(!SpellProgress.canUse(player, heal.definition().spellId()), "Fixed levels do not change independent unlock policy");
            SpellLevelConfig.LEARNING_MODE.set(SpellLevelConfig.LearningMode.ARS);
            h.assertTrue(form.shouldShowInUnlock() && !cap.knowsGlyph(heal), "Ars mode exposes learning without auto-teaching");
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(heal.getGlyph()));
            heal.getGlyph().use(h.getLevel(), player, InteractionHand.MAIN_HAND);
            h.assertTrue(cap.knowsGlyph(heal) && SpellProgress.craftedLevel(player, heal.definition().spellId()) == 0,
                    "Ars glyph item learns without inventing scroll history");
            var pos = new BlockPos(6, 1, 5);
            h.setBlock(pos, com.hollingsworth.arsnouveau.setup.registry.BlockRegistry.SCRIBES_BLOCK.get().defaultBlockState()
                    .setValue(ScribesBlock.PART, ThreePartBlock.HEAD));
            var table = (ScribesTile) h.getBlockEntity(pos);
            var holder = h.getLevel().getRecipeManager().byKey(ResourceLocation.parse("irons_nouveau:glyph_firebolt")).orElseThrow();
            @SuppressWarnings("unchecked") var recipe = (net.minecraft.world.item.crafting.RecipeHolder<GlyphRecipe>)(Object)holder;
            player.giveExperiencePoints(100);
            SpellLevelConfig.LEARNING_MODE.set(SpellLevelConfig.LearningMode.SCROLL_CRAFTING);
            table.setRecipe(recipe, player);
            h.assertTrue(table.recipe == null, "Forged recipe request cannot bypass scroll-crafting mode");
            SpellLevelConfig.LEARNING_MODE.set(SpellLevelConfig.LearningMode.ARS);
            table.setRecipe(recipe, player);
            h.assertTrue(table.recipe != null, "Native Scribe accepts recipe in Ars mode");
            var matching = scroll("firebolt", fire.getMaxLevel());
            h.assertTrue(recipe.value().inputs.getFirst().test(matching)
                    && !recipe.value().inputs.getFirst().test(scroll("icicle", 1)), "Recipe matches spell and accepts looted high-level scrolls");
            h.assertTrue(table.consumeStack(matching), "Real Scribe consumes matching scroll");
            var gem = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(ResourceLocation.parse("ars_nouveau:source_gem")));
            h.assertTrue(table.consumeStack(gem) && table.getRemainingRequired().isEmpty(), "Real Scribe completes required ingredients");
            var mana = CapabilityRegistry.getMana(player); mana.setMaxMana(10000); mana.setMana(10000);
            var iron = MagicData.getPlayerMagicData(player); iron.setMana(50);
            player.setHealth(1);
            var healingSpell = new Spell(MethodSelf.INSTANCE, heal);
            var context = new SpellContext(h.getLevel(), healingSpell, player, LivingCaster.from(player), ItemStack.EMPTY);
            int healingCost = SpellRegistry.getSpell(heal.definition().spellId()).getManaCost(4);
            double before = mana.getCurrentMana();
            heal.onResolve(new EntityHitResult(player), h.getLevel(), player, new SpellStats.Builder().build(), context, new SpellResolver(context));
            h.assertTrue(player.getHealth() > 1, "Native healing applies in independent runtime");
            close(h, mana.getCurrentMana(), before - healingCost, "Trigger spends real Ars mana exactly once");
            close(h, iron.getMana(), 50, "Separate Iron mana untouched without shared-pool addon");
            player.setHealth(1); mana.setMana(healingCost - 1);
            heal.onResolve(new EntityHitResult(player), h.getLevel(), player, new SpellStats.Builder().build(), context, new SpellResolver(context));
            close(h, player.getHealth(), 1, "Insufficient Ars mana rejects the effect");
            var parts = new ArrayList<AbstractSpellPart>(List.of(MethodProjectile.INSTANCE, form));
            for (int i = 0; i < 4; i++) parts.add(AugmentSplit.INSTANCE);
            var fan = new Spell(parts);
            var fanResolver = new SpellResolver(new SpellContext(h.getLevel(), fan, player, LivingCaster.from(player), ItemStack.EMPTY));
            int shotCost = fire.getManaCost(3);
            mana.setMana(fanResolver.getExpendedCost() + 3 * shotCost);
            h.assertTrue(fanResolver.onCast(ItemStack.EMPTY, h.getLevel()), "Real Ars player casts a fixed-level fan");
            var shots = h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, player.getBoundingBox().inflate(16), p -> p.getOwner() == player);
            h.assertTrue(shots.size() == 3, "Only three affordable native projectiles");
            close(h, mana.getCurrentMana(), 0, "Upfront Ars cost and native trigger costs share one real pool");
            for (var shot : shots) {
                var session = ((NativeCastCarrier)shot).ironsNouveau$session();
                h.assertTrue(session != null, "Native projectile has a managed session");
                session.finish(CastSession.EndReason.COMPLETED);
            }
            player.setGameMode(GameType.CREATIVE);
            close(h, SpellProgress.baseLevel(player, id), fire.getMaxLevel(), "Creative native maximum preserved by default");
            SpellLevelConfig.CREATIVE_NATIVE_MAX.set(false);
            close(h, SpellProgress.baseLevel(player, id), 3, "Creative can follow configured fixed level");
            player.setHealth(1); mana.setMana(0);
            heal.onResolve(new EntityHitResult(player), h.getLevel(), player, new SpellStats.Builder().build(), context, new SpellResolver(context));
            h.assertTrue(player.getHealth() > 1, "Creative still casts without mana");
            player.setGameMode(GameType.SURVIVAL);
            SpellLevelConfig.MODE.set(SpellLevelConfig.Mode.PERSONAL_SCROLL);
            SpellLevelConfig.LEARNING_MODE.set(SpellLevelConfig.LearningMode.SCROLL_CRAFTING);
            close(h, SpellProgress.baseLevel(player, id), 1, "Switching back restores preserved crafting history");
            h.assertTrue(cap.knowsGlyph(form) && !cap.knowsGlyph(heal), "Independent unlock policy restored");
            h.assertTrue(!SpellLevelConfig.validEntry("irons_spellbooks:firebolt=0")
                    && !SpellLevelConfig.validEntry("irons_nouveau:glyph_firebolt=3")
                    && !SpellLevelConfig.validEntry("irons_spellbooks:firebolt=bogus"), "Malformed override entries rejected");
        } finally {
            SpellLevelConfig.MODE.set(previousMode); SpellLevelConfig.DEFAULT_LEVEL.set(previousDefault);
            SpellLevelConfig.SPELL_LEVELS.set(previousOverrides); SpellLevelConfig.CREATIVE_NATIVE_MAX.set(previousCreative);
            SpellLevelConfig.LEARNING_MODE.set(previousLearning); player.discard();
        }
        h.succeed();
    }
}
