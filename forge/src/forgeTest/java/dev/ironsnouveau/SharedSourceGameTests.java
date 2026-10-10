package dev.ironsnouveau;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("irons_nouveau_shared") @PrefixGameTestTemplate(false)
public final class SharedSourceGameTests {
    @GameTest(template = "empty", timeoutTicks = 20)
    public static void unboundContextsAndInvalidLeases(GameTestHelper helper) {
        SharedSourceChecks.unboundContextsAndInvalidLeases(helper);
    }

    @GameTest(template = "empty", timeoutTicks = 20)
    public static void boundToolPersistenceAndScope(GameTestHelper helper) {
        SharedSourceChecks.boundToolPersistenceAndScope(helper);
    }
}
