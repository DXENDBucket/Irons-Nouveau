package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("irons_nouveau_storm") @PrefixGameTestTemplate(false)
public final class ThunderstormOwnershipGameTests {
    @GameTest(template="empty",batch="storm_transfer")
    public static void transferredStormKeepsCaster(GameTestHelper h) throws Exception {dev.ironsnouveau.tests.ThunderstormCases.transferredStormKeepsCasterAndNativeStrike(h);}
    @GameTest(template="empty",batch="storm_native")
    public static void nativeStormStaysNative(GameTestHelper h){dev.ironsnouveau.tests.ThunderstormCases.nativeStormWithoutBridgeMetadataStaysNative(h);}
}
