package dev.ironsnouveau.compat;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import net.minecraftforge.fml.ModList;

/** Optional Linker identification without linking or loading any Hex classes. */
public final class HexArsLinkCompat {
    private static final ClassValue<Boolean> LINKER_RESOLVERS = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            for (Class<?> current = type; current != null; current = current.getSuperclass()) {
                if (current.getName().equals("io.yukkuric.hex_ars_link.env.ars.PatternResolver")) return true;
            }
            return false;
        }
    };

    private HexArsLinkCompat() {}

    public static boolean isResolver(SpellResolver resolver) {
        return resolver != null && ModList.get().isLoaded("hex_ars_link")
                && LINKER_RESOLVERS.get(resolver.getClass());
    }
}
