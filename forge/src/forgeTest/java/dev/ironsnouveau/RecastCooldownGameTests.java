package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
import net.minecraft.server.level.ServerPlayer;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
@GameTestHolder("irons_nouveau_recast_cooldown") @PrefixGameTestTemplate(false)
public final class RecastCooldownGameTests {
    @GameTest(template="empty") public static void nativeThenUniversal(GameTestHelper h) {
        var p=player(h);p.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION.get()).setBaseValue(1.5);
        p.getAttribute(dev.arsconflux.platform.TimingAttributes.COOLDOWN_REDUCTION.get()).setBaseValue(.25);
        RecastCooldownCases.nativeThenUniversal(h,p);
    }
    private static ServerPlayer player(GameTestHelper h) {
        var p=ForgeSmokeTests.player(h); CapabilityRegistry.getMana(p).ifPresent(m -> {m.setMaxMana(10000);m.setMana(10000);});
        p.getAttribute(AttributeRegistry.MAX_MANA.get()).setBaseValue(10000); MagicData.getPlayerMagicData(p).setMana(10000); return p;
    }
    @GameTest(template="empty") public static void actualSingleShotAndNativeEvents(GameTestHelper h) {
        var p=player(h); p.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION.get()).setBaseValue(1.5);
        RecastCooldownCases.actualSingleShotAndNativeEvents(h,p,l -> net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(l),l -> net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(l));
    }
    @GameTest(template="empty") public static void resolvedLevelsAndEventOverrides(GameTestHelper h) {
        RecastCooldownCases.resolvedLevelsAndEventOverrides(h,player(h),l -> net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(l),l -> net.minecraftforge.common.MinecraftForge.EVENT_BUS.unregister(l));
    }
}
