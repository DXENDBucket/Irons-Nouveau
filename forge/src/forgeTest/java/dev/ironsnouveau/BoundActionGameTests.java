package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau_bound_action") @PrefixGameTestTemplate(false)
public final class BoundActionGameTests {
 @GameTest(template="empty",timeoutTicks=40) public static void boundToolBusyClaims(GameTestHelper h){BoundActionCases.busyClaims(h);}
}
