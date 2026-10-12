package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("irons_nouveau_anchors") @PrefixGameTestTemplate(false)
public final class AnchorMigrationGameTests {
    @GameTest(template="empty",timeoutTicks=40) public static void remoteSiphonAndChannel(GameTestHelper h){AnchorMigrationCases.remoteSiphon(h);}
    @GameTest(template="empty",timeoutTicks=40) public static void remoteRayAndWalls(GameTestHelper h){AnchorMigrationCases.rayQuery(h);}
    @GameTest(template="empty",timeoutTicks=70) public static void actualNegAndDelay(GameTestHelper h) throws Exception{AnchorMigrationCases.negAndDelay(h);}
}
