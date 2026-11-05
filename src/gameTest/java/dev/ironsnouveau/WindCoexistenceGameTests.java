package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("irons_nouveau_wind_neg") @PrefixGameTestTemplate(false)
public final class WindCoexistenceGameTests {
    @GameTest(template="empty",timeoutTicks=60) public static void splash(GameTestHelper h){WindManualCases.splash(h);}
}
