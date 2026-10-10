package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau_step_aim")
@PrefixGameTestTemplate(false)
public final class StepAimGameTests {
    @GameTest(template="empty") public static void stepEntrances(GameTestHelper h) { SelectedEntityTestCases.steps(h); }
}
