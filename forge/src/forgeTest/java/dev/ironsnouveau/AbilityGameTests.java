package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau_abilities") @PrefixGameTestTemplate(false)
public final class AbilityGameTests {
    @GameTest(template="empty") public static void registered(GameTestHelper h){dev.ironsnouveau.tests.AbilityCases.registered(h);}
    @GameTest(template="empty",timeoutTicks=320) public static void rift(GameTestHelper h){dev.ironsnouveau.tests.AbilityCases.rift(h);}
    @GameTest(template="empty") public static void zone(GameTestHelper h){dev.ironsnouveau.tests.AbilityCases.zone(h);}
    @GameTest(template="empty") public static void wall(GameTestHelper h){dev.ironsnouveau.tests.AbilityCases.wall(h);}
    @GameTest(template="empty") public static void gust(GameTestHelper h){dev.ironsnouveau.tests.AbilityCases.gust(h);}
    @GameTest(template="empty",timeoutTicks=90) public static void dagger(GameTestHelper h){dev.ironsnouveau.tests.AbilityCases.dagger(h);}
}
