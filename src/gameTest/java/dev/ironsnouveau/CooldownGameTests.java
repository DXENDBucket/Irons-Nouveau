package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHeal;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.progression.SpellProgress;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@GameTestHolder("irons_nouveau_cooldown")
@PrefixGameTestTemplate(false)
public final class CooldownGameTests {
    private static BridgeGlyph glyph(String id) {
        return IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow();
    }
    private static void learn(ServerPlayer player, AbstractSpell spell) {
        var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, scroll); SpellProgress.crafted(player, scroll);
    }
    private static ServerPlayer player(GameTestHelper h) {
        var player = ScrollProgressGameTests.player(h);
        var mana = CapabilityRegistry.getMana(player); mana.setMaxMana(10000); mana.setMana(10000);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)); player.setXRot(-90);
        return player;
    }
    private static void cast(GameTestHelper h, ServerPlayer player, Spell spell) {
        new SpellCaster().castSpell(h.getLevel(), player, InteractionHand.MAIN_HAND, null, spell);
    }
    @GameTest(template = "empty", batch = "cooldown_native")
    public static void nativeStorageReductionEventsAndOptOut(GameTestHelper h) {
        var player = player(h); var heal = glyph("heal"); var nativeHeal = SpellRegistry.getSpell(heal.definition().spellId());
        learn(player, nativeHeal);
        var cds = MagicData.getPlayerMagicData(player).getPlayerCooldowns();
        var recipe = new Spell(MethodSelf.INSTANCE, heal, heal);
        boolean oldEnabled = SpellLevelConfig.COOLDOWNS_ENABLED.get(), oldChant = SpellLevelConfig.CHANTING_ENABLED.get();
        boolean oldCreative = ServerConfigs.CREATIVE_COOLDOWN.get();
        var count = new AtomicInteger();
        Consumer<SpellCooldownAddedEvent.Post> listener = event -> { if (event.getEntity() == player) count.incrementAndGet(); };
        NeoForge.EVENT_BUS.addListener(listener);
        try {
            h.assertTrue(SpellLevelConfig.COOLDOWNS_ENABLED.getDefault(), "Feature defaults on");
            SpellLevelConfig.COOLDOWNS_ENABLED.set(true); SpellLevelConfig.CHANTING_ENABLED.set(false);
            player.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION).setBaseValue(1.5);
            player.setHealth(1); cast(h, player, recipe);
            h.assertTrue(player.getHealth() > 1 && cds.isOnCooldown(nativeHeal), "Real active heal writes native cooldown");
            int expected = MagicManager.getEffectiveSpellCooldown(nativeHeal, player, CastSource.SPELLBOOK);
            h.assertTrue(cds.getSpellCooldowns().get(nativeHeal.getSpellId()).getCooldownRemaining() == expected && count.get() == 1,
                    "Native reduction and event used once despite duplicate glyphs");
            player.setHealth(1); cast(h, player, new Spell(MethodSelf.INSTANCE, heal));
            h.assertTrue(player.getHealth() == 1 && count.get() == 1, "Different recipe cannot bypass shared native cooldown");
            cast(h, player, new Spell(MethodSelf.INSTANCE, EffectHeal.INSTANCE));
            h.assertTrue(player.getHealth() > 1 && count.get() == 1, "Pure Ars remains usable");
            cds.clearCooldowns(); MagicHelper.MAGIC_MANAGER.addCooldown(player, nativeHeal, CastSource.SPELLBOOK);
            player.setHealth(1); cast(h, player, recipe);
            h.assertTrue(player.getHealth() == 1, "Cooldown created by Iron blocks bridge");
            SpellLevelConfig.COOLDOWNS_ENABLED.set(false); cast(h, player, recipe);
            h.assertTrue(player.getHealth() > 1 && count.get() == 2, "Disabled bridge ignores cooldown and adds none");
            SpellLevelConfig.COOLDOWNS_ENABLED.set(true); ServerConfigs.CREATIVE_COOLDOWN.set(false);
            player.setGameMode(GameType.CREATIVE); player.setHealth(1); cast(h, player, recipe);
            h.assertTrue(player.getHealth() > 1 && count.get() == 2, "Creative follows Iron exemption");
            ServerConfigs.CREATIVE_COOLDOWN.set(true); player.setHealth(1); cast(h, player, recipe);
            h.assertTrue(player.getHealth() == 1, "Iron creative cooldown opt-in respected");
            player.setGameMode(GameType.SURVIVAL); cds.clearCooldowns();
            Consumer<SpellCooldownAddedEvent.Pre> modify = event -> { if (event.getEntity() == player) event.setEffectiveCooldown(7); };
            NeoForge.EVENT_BUS.addListener(modify);
            try { cast(h, player, recipe); h.assertTrue(cds.getSpellCooldowns().get(nativeHeal.getSpellId()).getCooldownRemaining() == 7, "Native pre event may modify cooldown"); }
            finally { NeoForge.EVENT_BUS.unregister(modify); }
            cds.tick(7); h.assertTrue(ActiveCooldowns.allowed(recipe, player), "Native expiry re-enables casting");
            h.succeed();
        } finally {
            NeoForge.EVENT_BUS.unregister(listener); player.discard();
            SpellLevelConfig.COOLDOWNS_ENABLED.set(oldEnabled); SpellLevelConfig.CHANTING_ENABLED.set(oldChant);
            ServerConfigs.CREATIVE_COOLDOWN.set(oldCreative);
        }
    }
    @GameTest(template = "empty", batch = "cooldown_branches", timeoutTicks = 120)
    public static void splitAndOngoingTriggersSurviveOwnCooldown(GameTestHelper h) {
        var player = player(h); var nativeBolt = SpellRegistry.FIREBOLT_SPELL.get(); learn(player, nativeBolt);
        var form = IronsNouveau.forms().stream().filter(f -> f.spellId().equals(nativeBolt.getSpellResource())).findFirst().orElseThrow();
        var oldEnabled = SpellLevelConfig.COOLDOWNS_ENABLED.get(); var oldChant = SpellLevelConfig.CHANTING_ENABLED.get();
        SpellLevelConfig.COOLDOWNS_ENABLED.set(true); SpellLevelConfig.CHANTING_ENABLED.set(false);
        try {
            var recipe = new Spell(MethodProjectile.INSTANCE, form, AugmentSplit.INSTANCE, AugmentSplit.INSTANCE);
            cast(h, player, recipe);
            var shots = h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, player.getBoundingBox().inflate(20), p -> p.getOwner() == player);
            h.assertTrue(shots.size() == 3, "All three split projectiles spawn before cooldown commits: " + shots.size());
            var cds = MagicData.getPlayerMagicData(player).getPlayerCooldowns();
            h.assertTrue(cds.isOnCooldown(nativeBolt), "One shared cooldown started");
            cast(h, player, recipe);
            h.assertTrue(h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, player.getBoundingBox().inflate(20), p -> p.getOwner() == player).size() == 3, "Second cast blocked as a whole");
            var heal = glyph("heal"); var nativeHeal = SpellRegistry.getSpell(heal.definition().spellId()); learn(player, nativeHeal);
            var laterRecipe = new Spell(MethodSelf.INSTANCE, heal);
            var context = new SpellContext(h.getLevel(), laterRecipe, player, com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster.from(player), ItemStack.EMPTY);
            cds.addCooldown(nativeHeal, 200); player.setHealth(1);
            double before = CapabilityRegistry.getMana(player).getCurrentMana();
            // Deferred hits/continuations enter resolution, not a fresh active-use boundary.
            new SpellResolver(context).onResolveEffect(h.getLevel(), new net.minecraft.world.phys.EntityHitResult(player));
            h.assertTrue(player.getHealth() > 1 && CapabilityRegistry.getMana(player).getCurrentMana() < before,
                    "Ongoing trigger still applies and spends mana during its own cooldown");
            shots.forEach(net.minecraft.world.entity.Entity::discard);
            var crossbowStack = new ItemStack(net.minecraft.core.registries.BuiltInRegistries.ITEM.get(
                    net.minecraft.resources.ResourceLocation.parse("ars_nouveau:spell_crossbow")));
            var crossbow = (com.hollingsworth.arsnouveau.common.items.SpellCrossbow)crossbowStack.getItem();
            new SpellCaster().setSpell(new Spell(MethodProjectile.INSTANCE, form)).saveToStack(crossbowStack);
            var ammo = net.minecraft.world.item.component.ChargedProjectiles.of(new ItemStack(Items.ARROW));
            crossbowStack.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES, ammo);
            var tag = new net.minecraft.nbt.CompoundTag(); tag.putBoolean("isSpell", true);
            crossbowStack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
            player.setItemInHand(InteractionHand.MAIN_HAND, crossbowStack);
            crossbow.performShooting(h.getLevel(), player, InteractionHand.MAIN_HAND, crossbowStack, 3, 0, null);
            h.assertTrue(crossbowStack.get(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES) == ammo,
                    "Blocked preloaded crossbow keeps its ammo");
            cds.clearCooldowns();
            crossbow.performShooting(h.getLevel(), player, InteractionHand.MAIN_HAND, crossbowStack, 3, 0, null);
            h.assertTrue(crossbowStack.get(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES).isEmpty()
                    && cds.isOnCooldown(nativeBolt), "Successful crossbow shot consumes ammo and commits cooldown");
            h.succeed();
        } finally { player.discard(); SpellLevelConfig.COOLDOWNS_ENABLED.set(oldEnabled); SpellLevelConfig.CHANTING_ENABLED.set(oldChant); }
    }
    @GameTest(template = "empty", batch = "cooldown_chant", timeoutTicks = 100)
    public static void interruptedAndInvalidCastsDoNotStartCooldown(GameTestHelper h) {
        var player = player(h); var vex = glyph("summon_vex"); var nativeVex = SpellRegistry.getSpell(vex.definition().spellId()); learn(player, nativeVex);
        boolean oldEnabled = SpellLevelConfig.COOLDOWNS_ENABLED.get(), oldChant = SpellLevelConfig.CHANTING_ENABLED.get();
        SpellLevelConfig.COOLDOWNS_ENABLED.set(true); SpellLevelConfig.CHANTING_ENABLED.set(true);
        var recipe = new Spell(MethodSelf.INSTANCE, vex);
        var cds = MagicData.getPlayerMagicData(player).getPlayerCooldowns();
        cast(h, player, recipe);
        h.assertTrue(ActiveChanting.isChanting(player) && !cds.isOnCooldown(nativeVex), "Chant alone starts no cooldown");
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
        h.runAfterDelay(3, () -> {
            try {
                h.assertTrue(!ActiveChanting.isChanting(player) && !cds.isOnCooldown(nativeVex), "Cancelled chant starts no cooldown");
                cds.addCooldown(nativeVex, 200); cast(h, player, recipe);
                h.assertTrue(!ActiveChanting.isChanting(player), "Cooldown checked before beginning chant");
                cds.clearCooldowns(); SpellLevelConfig.CHANTING_ENABLED.set(false);
                CapabilityRegistry.getMana(player).setMana(0);
                cast(h, player, new Spell(MethodProjectile.INSTANCE, vex));
                h.assertTrue(!cds.isOnCooldown(nativeVex), "Failed mana validation starts no cooldown");
                h.succeed();
            } finally { player.discard(); SpellLevelConfig.COOLDOWNS_ENABLED.set(oldEnabled); SpellLevelConfig.CHANTING_ENABLED.set(oldChant); }
        });
    }
}
