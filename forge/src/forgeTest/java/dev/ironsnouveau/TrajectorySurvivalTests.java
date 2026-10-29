package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau_contacts") @PrefixGameTestTemplate(false)
public final class TrajectorySurvivalTests {
    @GameTest(template="empty",batch="contact2",timeoutTicks=100) public static void orbitCollisionAndLifetime(GameTestHelper h){TrajectorySurvivalCases.orbitRetainsItsControllerAfterAnActualEntityContact(h);}
    @GameTest(template="empty",batch="contact3",timeoutTicks=100) public static void terminalConditions(GameTestHelper h){TrajectorySurvivalCases.orbitWallAndOwnerRemovalRemainTerminal(h);}
    @GameTest(template="empty",batch="contact4",timeoutTicks=100) public static void legacyPaths(GameTestHelper h){TrajectorySurvivalCases.ordinaryBallAndNonpersistentOrbitRemainNative(h);}
}
