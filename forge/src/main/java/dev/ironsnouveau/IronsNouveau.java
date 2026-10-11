package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import dev.ironsnouveau.api.SpellAdapter;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.glyph.NativeFormAugment;
import dev.ironsnouveau.casting.NativeCasting;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import java.util.List;

@Mod(IronsNouveau.MOD_ID)
public final class IronsNouveau {
    public static final String MOD_ID = "irons_nouveau";
    public IronsNouveau() {
        IEventBus bus = net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus();
        dev.ironsnouveau.platform.BoundSpellData.register(bus);
        dev.ironsnouveau.network.ForgeNetwork.register();
        net.minecraftforge.fml.ModLoadingContext.get().registerConfig(net.minecraftforge.fml.config.ModConfig.Type.SERVER,
                dev.ironsnouveau.config.SpellLevelConfig.SPEC, "irons_nouveau-server.toml");
        dev.ironsnouveau.progression.SpellProgress.register(bus, MinecraftForge.EVENT_BUS);
        dev.ironsnouveau.progression.NoNaturalGlyphs.register(bus);
        dev.ironsnouveau.recipe.SpellScrollIngredient.register(bus);
        dev.arsconflux.api.glyph.SpellValidators.register(
                dev.arsconflux.api.projectile.CarrierRegistry.id("irons_nouveau:projectiles"), NativeCasting::validate);
        GlyphCatalog.register();
        dev.ironsnouveau.casting.IronActiveCastProvider.register();
        bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(GlyphCatalog::configureCarriers));
        dev.ironsnouveau.platform.CastingLifecycle.register(MinecraftForge.EVENT_BUS);
        MinecraftForge.EVENT_BUS.addListener(NativeCasting::beforeCast);
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
