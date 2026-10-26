package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau_native_self") @PrefixGameTestTemplate(false)
public final class NativeSelfGameTests {
    private static void run(GameTestHelper h,java.util.function.BiConsumer<GameTestHelper,net.minecraft.server.level.ServerPlayer> test) {
        var player=ForgeSmokeTests.player(h);NativeSelfCases.online(player,()->test.accept(h,player));
    }
    @GameTest(template="empty") public static void registrationAndRecipes(GameTestHelper h){NativeSelfCases.registration(h);}
    @GameTest(template="empty") public static void instantAndPreconditions(GameTestHelper h){run(h,NativeSelfCases::instantAndPreconditions);}
    @GameTest(template="empty") public static void preparedAndNativePool(GameTestHelper h){run(h,NativeSelfCases::preparedAndNativePool);}
    @GameTest(template="empty") public static void channelAndCleanup(GameTestHelper h){run(h,NativeSelfCases::channel);}
    @GameTest(template="empty") public static void nativeBaseline(GameTestHelper h){run(h,NativeSelfCases::untouchedNativeCast);}
}
