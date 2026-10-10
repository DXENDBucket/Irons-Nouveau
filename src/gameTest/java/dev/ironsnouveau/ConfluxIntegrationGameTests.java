package dev.ironsnouveau;

import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;

/** Focused extraction regressions; reuse existing behavioral tests without broadening the run. */
@GameTestHolder("irons_nouveau_conflux") @PrefixGameTestTemplate(false)
public final class ConfluxIntegrationGameTests {
    @GameTest(template="empty", timeoutTicks=100)
    public static void optionalPropagators(GameTestHelper h) { PropagationGameTests.allPropagatorsReplaceAndPreserveRemainingEffects(h); }
    @GameTest(template="empty", timeoutTicks=100)
    public static void impactStartsAnotherPayload(GameTestHelper h) { PropagationGameTests.nativeImpactPropagatesAnotherNativeProjectile(h); }
    @GameTest(template="empty", timeoutTicks=100)
    public static void repeatedTrailPropagation(GameTestHelper h) { PropagationGameTests.echoingTrailEmitsNativeShotsRepeatedly(h); }
    @GameTest(template="empty", timeoutTicks=100)
    public static void affordableSymmetricFan(GameTestHelper h) { TriggerGameTests.fiveShotsBecomeNativeThreeShotFan(h); }
    @GameTest(template="empty", timeoutTicks=100)
    public static void successfulCollisionContinuesOnce(GameTestHelper h) { BridgeGameTests.realCollisionResumesOnceWithoutSecondPayment(h); }
    @GameTest(template="empty", timeoutTicks=100)
    public static void targetBreathKeepsOwnerAndPayment(GameTestHelper h) { BreathGameTests.targetOwnsPoseButCasterOwnsDamageAndMana(h); }
    @GameTest(template="empty", timeoutTicks=100)
    public static void creativeBreathAndSessionExpiry(GameTestHelper h) { BreathGameTests.allConeTypesExpireWithoutUsingNativeCastingState(h); }
}
