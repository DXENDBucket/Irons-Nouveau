package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau_wind") @PrefixGameTestTemplate(false)
public final class WindManualGameTests {
    @GameTest(template="empty") public static void registered(GameTestHelper h){WindManualCases.registration(h);}
    @GameTest(template="empty") public static void tornado(GameTestHelper h){WindManualCases.tornado(h);}
    @GameTest(template="empty") public static void push(GameTestHelper h){WindManualCases.push(h);}
    @GameTest(template="empty") public static void tailwind(GameTestHelper h){WindManualCases.tailwind(h);}
    @GameTest(template="empty") public static void jump(GameTestHelper h){WindManualCases.jump(h);}
    @GameTest(template="empty") public static void actions(GameTestHelper h){WindManualCases.actions(h);}
    @GameTest(template="empty",timeoutTicks=50) public static void blade(GameTestHelper h){WindManualCases.blade(h);}
    @GameTest(template="empty",timeoutTicks=90) public static void orbit(GameTestHelper h){WindManualCases.orbit(h);}
}
