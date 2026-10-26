package dev.ironsnouveau.compat;

import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Only registration identities, never factories or early spell instances. */
public final class NativeSpellIndex {
    private static final Set<ResourceLocation> IDS=java.util.concurrent.ConcurrentHashMap.newKeySet();
    private NativeSpellIndex() {}
    public static void announced(ResourceLocation registry,ResourceLocation id) {
        if(registry.toString().equals("irons_spellbooks:spells")
                &&!Set.of("irons_spellbooks","ars_n_spells","irons_nouveau").contains(id.getNamespace())) IDS.add(id);
    }
    public static List<ResourceLocation> spells() { return IDS.stream().sorted(Comparator.comparing(ResourceLocation::toString)).toList(); }
    public static ResourceLocation glyphId(ResourceLocation spell) {
        return dev.ironsnouveau.platform.Locations.id("irons_nouveau:glyph_native/"+spell.getNamespace()+"/"+spell.getPath());
    }
}
