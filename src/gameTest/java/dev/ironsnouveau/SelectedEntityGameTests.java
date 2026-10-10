package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("irons_nouveau_selection")
@PrefixGameTestTemplate(false)
public final class SelectedEntityGameTests {
    @GameTest(template="empty") public static void steps(GameTestHelper h) { SelectedEntityTestCases.steps(h); }
    @GameTest(template="empty", timeoutTicks=40) public static void telekinesis(GameTestHelper h) { SelectedEntityTestCases.telekinesis(h); }
    @GameTest(template="empty") public static void shackle(GameTestHelper h) { SelectedEntityTestCases.shackle(h); }
}
