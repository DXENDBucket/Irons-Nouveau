package dev.ironsnouveau.platform;

import dev.ironsnouveau.GlyphCatalog;
import dev.ironsnouveau.compat.NativeSpellIndex;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.RegisterEvent;

public final class NativeSelfRegistration {
    private NativeSelfRegistration() {}
    public static void register(IEventBus bus) { bus.addListener(NativeSelfRegistration::items); }
    private static void items(RegisterEvent event) {
        if(!event.getRegistryKey().equals(Registries.ITEM))return;
        for(var id:NativeSpellIndex.spells()) {
            var glyph=GlyphCatalog.addNative(id);
            if(glyph!=null)event.register(Registries.ITEM,glyph.getRegistryName(),glyph::getGlyph);
        }
    }
}
