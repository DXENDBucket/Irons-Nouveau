package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import net.minecraft.server.level.ServerPlayer;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
@GameTestHolder("irons_nouveau_recast_cooldown") @PrefixGameTestTemplate(false)
public final class RecastCooldownGameTests {
    @GameTest(template="empty") public static void nativeThenUniversal(GameTestHelper h) {
        var p=player(h);p.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION).setBaseValue(1.5);
        p.getAttribute(dev.arsconflux.platform.TimingAttributes.COOLDOWN_REDUCTION).setBaseValue(.25);
        RecastCooldownCases.nativeThenUniversal(h,p);
    }
    private static ServerPlayer player(GameTestHelper h) {
        var p=ScrollProgressGameTests.player(h); var m=CapabilityRegistry.getMana(p); m.setMaxMana(10000); m.setMana(10000);
        p.getAttribute(AttributeRegistry.MAX_MANA).setBaseValue(10000); MagicData.getPlayerMagicData(p).setMana(10000); return p;
    }
    @GameTest(template="empty") public static void actualSingleShotAndNativeEvents(GameTestHelper h) {
        var p=player(h); p.getAttribute(AttributeRegistry.COOLDOWN_REDUCTION).setBaseValue(1.5);
        RecastCooldownCases.actualSingleShotAndNativeEvents(h,p,l -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(l),l -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(l));
    }
    @GameTest(template="empty") public static void resolvedLevelsAndEventOverrides(GameTestHelper h) {
        RecastCooldownCases.resolvedLevelsAndEventOverrides(h,player(h),l -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(l),l -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(l));
    }
}
