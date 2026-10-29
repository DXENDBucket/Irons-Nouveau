package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("irons_nouveau_contacts") @PrefixGameTestTemplate(false)
public final class TrajectorySurvivalTests {
    @GameTest(template="empty",batch="contact0",timeoutTicks=100) public static void splashFlightAndBatch(GameTestHelper h){TrajectorySurvivalCases.splashContinuesItsActualFlightAndSharesContactPayment(h);}
    @GameTest(template="empty",batch="contact1",timeoutTicks=100) public static void intervalAndMana(GameTestHelper h){TrajectorySurvivalCases.contactIntervalAndEmptyManaPreserveFlightWithoutFreeEffects(h);}
    @GameTest(template="empty",batch="contact2",timeoutTicks=100) public static void orbitCollisionAndLifetime(GameTestHelper h){TrajectorySurvivalCases.orbitRetainsItsControllerAfterAnActualEntityContact(h);}
    @GameTest(template="empty",batch="contact3",timeoutTicks=100) public static void terminalConditions(GameTestHelper h){TrajectorySurvivalCases.wallAndOwnerRemovalRemainTerminal(h);}
    @GameTest(template="empty",batch="contact4",timeoutTicks=100) public static void legacyPaths(GameTestHelper h){TrajectorySurvivalCases.ordinaryBallAndNonpersistentPayloadRemainNative(h);}
}
