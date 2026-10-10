package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("irons_nouveau_telekinesis")
@PrefixGameTestTemplate(false)
public final class TelekinesisGameTests {
    @GameTest(template="empty", timeoutTicks=40) public static void playerChannel(GameTestHelper h) {
        var player = ScrollProgressGameTests.player(h);
        player.setData(dev.ironsnouveau.progression.SpellProgress.CRAFTED,
                dev.ironsnouveau.progression.CraftedSpells.EMPTY.record(
                        dev.ironsnouveau.platform.Locations.id("irons_spellbooks", "telekinesis"), 1));
        SelectedEntityTestCases.telekinesisPlayer(h, player);
    }
}
