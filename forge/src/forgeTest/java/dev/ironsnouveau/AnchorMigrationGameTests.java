package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau_anchors") @PrefixGameTestTemplate(false)
public final class AnchorMigrationGameTests {
    @GameTest(template="empty",timeoutTicks=40) public static void remoteSiphonAndChannel(GameTestHelper h){AnchorMigrationCases.remoteSiphon(h);}
    @GameTest(template="empty",timeoutTicks=40) public static void remoteRayAndWalls(GameTestHelper h){AnchorMigrationCases.rayQuery(h);}
}
