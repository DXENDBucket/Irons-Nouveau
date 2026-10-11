package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("irons_nouveau_beneficial") @PrefixGameTestTemplate(false)
public final class BeneficialExecutorGameTests {
    @GameTest(template="empty",timeoutTicks=40) public static void hostAuthorizationAndCancellation(GameTestHelper h){BeneficialExecutorCases.hostAuthorizationAndCancellation(h);}
    @GameTest(template="empty") public static void casterStepsStillUseEnemyDestinations(GameTestHelper h){SelectedEntityTestCases.steps(h);}
}
