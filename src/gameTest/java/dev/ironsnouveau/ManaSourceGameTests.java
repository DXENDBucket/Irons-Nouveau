package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.config.SpellLevelConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("irons_nouveau_mana")
@PrefixGameTestTemplate(false)
public final class ManaSourceGameTests {
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void selectedPoolPaymentsAndFallback(GameTestHelper h) {
        var player = ScrollProgressGameTests.player(h);
        player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA).setBaseValue(10000);
        var ars = CapabilityRegistry.getMana(player); ars.setMaxMana(10000);
        var iron = MagicData.getPlayerMagicData(player);
        var spell = SpellRegistry.FIREBOLT_SPELL.get(); var id = spell.getSpellResource();
        int cost = spell.getManaCost(1);
        var bill = TriggerMana.of(null, player);
        boolean previous = SpellLevelConfig.USE_IRON_MANA.get();
        try {
            h.assertTrue(previous, "Iron mana is enabled by default");
            ars.setMana(0); iron.setMana(cost * 2);
            h.assertTrue(!bill.trigger(id, 1, () -> false) && iron.getMana() == cost * 2, "Failed effects do not spend mana");
            h.assertTrue(bill.trigger(id, 1, () -> true), "Iron-funded trigger works with zero Ars mana");
            h.assertTrue(iron.getMana() == cost && ars.getCurrentMana() == 0, "Native cost charged exactly once");
            h.assertTrue(bill.trigger(id, 1, () -> {
                h.assertTrue(!bill.trigger(id, 1, () -> true), "Nested trigger cannot spend reserved mana");
                return true;
            }), "Second ongoing payment can consume remaining Iron mana");
            ars.setMana(cost * 3);
            h.assertTrue(!bill.trigger(id, 1, () -> true), "Empty Iron pool never falls back to stocked Ars pool");
            SpellLevelConfig.USE_IRON_MANA.set(false);
            h.assertTrue(bill.trigger(id, 1, () -> true) && ars.getCurrentMana() == cost * 2 && iron.getMana() == 0,
                    "Disabled option restores Ars payment only");
            SpellLevelConfig.USE_IRON_MANA.set(true);
            player.setGameMode(GameType.CREATIVE); ars.setMana(0); iron.setMana(0);
            h.assertTrue(bill.trigger(id, 1, () -> true) && bill.affordableCount(id, 1, 5) == 5,
                    "Creative ignores both empty pools");
        } finally { SpellLevelConfig.USE_IRON_MANA.set(previous); }
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 80)
    public static void ironBudgetControlsRealArsFan(GameTestHelper h) {
        var player = ScrollProgressGameTests.player(h); player.setYRot(-90);
        player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA).setBaseValue(10000);
        var ars = CapabilityRegistry.getMana(player); ars.setMaxMana(10000);
        var iron = MagicData.getPlayerMagicData(player);
        var form = IronsNouveau.forms().stream().filter(f -> f.spellId().getPath().equals("firebolt")).findFirst().orElseThrow();
        var nativeSpell = SpellRegistry.getSpell(form.spellId());
        var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        io.redspace.ironsspellbooks.api.spells.ISpellContainer.createScrollContainer(nativeSpell, 1, scroll);
        dev.ironsnouveau.progression.SpellProgress.crafted(player, scroll);
        var parts = new java.util.ArrayList<AbstractSpellPart>(java.util.List.of(MethodProjectile.INSTANCE, form));
        for (int i = 0; i < 4; i++) parts.add(AugmentSplit.INSTANCE);
        var resolver = new SpellResolver(SpellContext.fromEntity(new Spell(parts), player, ItemStack.EMPTY));
        ars.setMana(resolver.getExpendedCost()); iron.setMana(3 * nativeSpell.getManaCost(1));
        h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "Ars can pay its carrier cost independently");
        var shots = h.getLevel().getEntitiesOfClass(io.redspace.ironsspellbooks.entity.spells.firebolt.FireboltProjectile.class,
                player.getBoundingBox().inflate(16), p -> p.getOwner() == player);
        h.assertTrue(shots.size() == 3 && iron.getMana() == 0 && ars.getCurrentMana() == 0,
                "Five requested shots use a three-shot Iron budget and separate Ars carrier cost");
        var angles = shots.stream().map(p -> Math.toDegrees(Math.atan2(p.getDeltaMovement().z, p.getDeltaMovement().x))).sorted().toList();
        h.assertTrue(Math.abs(angles.get(0) + 10) < 3 && Math.abs(angles.get(1)) < 3 && Math.abs(angles.get(2) - 10) < 3,
                "Ars keeps a centered three-shot fan");
        for (var shot : shots) ((NativeCastCarrier)shot).ironsNouveau$session().finish(CastSession.EndReason.COMPLETED);
        h.succeed();
    }
}
