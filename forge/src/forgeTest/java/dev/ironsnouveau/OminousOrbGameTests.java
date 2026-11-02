package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau_orb") @PrefixGameTestTemplate(false)
public final class OminousOrbGameTests {
    @GameTest(template="empty") public static void placement(GameTestHelper h){dev.ironsnouveau.tests.OminousOrbCases.placement(h);}
    @GameTest(template="empty") public static void explosion(GameTestHelper h){dev.ironsnouveau.tests.OminousOrbCases.explosion(h);}
    @GameTest(template="empty") public static void destruction(GameTestHelper h){dev.ironsnouveau.tests.OminousOrbCases.destruction(h);}
    @GameTest(template="empty") public static void cleanup(GameTestHelper h){dev.ironsnouveau.tests.OminousOrbCases.cleanup(h);}
}
