package dev.ironsnouveau;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Focused extraction regression; reuses the real spell and resource checks. */
@GameTestHolder("irons_nouveau_conflux")
@PrefixGameTestTemplate(false)
public final class ConfluxIntegrationGameTests {
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeEffectsPayAtTrigger(GameTestHelper helper) {
        ForgeSmokeTests.registrationHealingAndMana(helper);
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void nativeProjectilesUseCore(GameTestHelper helper) {
        ForgeSmokeTests.nativeProjectileConversion(helper);
    }
    @GameTest(template = "empty", timeoutTicks = 100)
    public static void remoteFrostKeepsGeometryAndLifetime(GameTestHelper helper) {
        RaySpellsGameTests.frostRemoteEyesNativeDamageFreezingAndVisualLifetime(helper);
    }
    @GameTest(template = "empty", timeoutTicks = 160)
    public static void chantingAndCooldownRemainNative(GameTestHelper helper) {
        ForgeSmokeTests.activeChantAndCooldown(helper);
    }
}
