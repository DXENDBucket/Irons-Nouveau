package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import dev.ironsnouveau.api.SpellAdapter;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.glyph.NativeFormAugment;
import dev.ironsnouveau.casting.NativeCasting;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.List;

@Mod(IronsNouveau.MOD_ID)
public final class IronsNouveau {
    public static final String MOD_ID = "irons_nouveau";
    public IronsNouveau(IEventBus bus, net.neoforged.fml.ModContainer container) {
        dev.ironsnouveau.platform.BoundSpellData.register(bus);
        bus.addListener(dev.ironsnouveau.network.ChantStatePayload::register);
        bus.addListener(dev.ironsnouveau.network.SiphonRayVisualPayload::register);
        bus.addListener(dev.ironsnouveau.network.CastingMovementPayload::register);
        container.registerConfig(net.neoforged.fml.config.ModConfig.Type.SERVER,
                dev.ironsnouveau.config.SpellLevelConfig.SPEC, "irons_nouveau-server.toml");
        dev.ironsnouveau.progression.SpellProgress.register(bus, NeoForge.EVENT_BUS);
        dev.ironsnouveau.progression.NoNaturalGlyphs.register(bus);
        dev.ironsnouveau.recipe.SpellScrollIngredient.register(bus);
        dev.arsconflux.api.glyph.SpellValidators.register(
                dev.arsconflux.api.projectile.CarrierRegistry.id("irons_nouveau:projectiles"), NativeCasting::validate);
        GlyphCatalog.register();
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(GlyphCatalog::configureCarriers));
        dev.ironsnouveau.platform.CastingLifecycle.register(NeoForge.EVENT_BUS);
        NeoForge.EVENT_BUS.addListener(NativeCasting::beforeCast);
        var tabs = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
        tabs.register("glyphs", () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.irons_nouveau"))
                .icon(() -> GlyphCatalog.forms().get(0).getGlyph().getDefaultInstance())
                .displayItems((parameters, output) -> {
                    var order = dev.ironsnouveau.glyph.GlyphDisplayOrder.bySchool(
                            java.util.Comparator.comparing(part -> part.getRegistryName().toString()));
                    GlyphCatalog.forms().stream().sorted(order).forEach(glyph -> output.accept(glyph.getGlyph()));
                    GlyphCatalog.glyphs().stream().sorted(order).forEach(glyph -> output.accept(glyph.getGlyph()));
                })
                .build());
        tabs.register(bus);
    }
    public static List<BridgeGlyph> glyphs() { return GlyphCatalog.glyphs(); }
    public static List<NativeFormAugment> forms() { return GlyphCatalog.forms(); }
    static void add(String spell, String name, int cost, SpellTier tier, SpellSchool school,
                    boolean harmful, boolean duration, SpellAdapter adapter) {
        GlyphCatalog.add(spell, name, cost, tier, school, harmful, duration, adapter);
    }
}
