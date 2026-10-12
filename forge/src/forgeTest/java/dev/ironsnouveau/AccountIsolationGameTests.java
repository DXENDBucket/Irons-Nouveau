package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau_audit") @PrefixGameTestTemplate(false)
public final class AccountIsolationGameTests {
    @GameTest(template="empty",timeoutTicks=40)
    public static void crossProviderAccountDoesNotLeak(GameTestHelper h) {
        var p=ForgeSmokeTests.player(h);
        var mana=com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(p).orElseThrow(IllegalStateException::new);mana.setMaxMana(10000);
        AccountIsolationCases.verify(h,p,amount->mana.setMana(amount),()->mana.getCurrentMana());
    }
}
