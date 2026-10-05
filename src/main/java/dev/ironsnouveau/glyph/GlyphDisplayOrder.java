package dev.ironsnouveau.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Presentation only: retain category boundaries and upstream ordering within each source group. */
public final class GlyphDisplayOrder {
    private GlyphDisplayOrder() {}

    public static <T> Comparator<T> ironLast(Comparator<T> original, Function<T, AbstractSpellPart> part,
                                            ToIntFunction<AbstractSpellPart> category) {
        return Comparator.<T>comparingInt(value -> category.applyAsInt(part.apply(value)))
                .thenComparingInt(value -> part.apply(value).getRegistryName().getNamespace().equals("irons_nouveau") ? 1 : 0)
                .thenComparing(original);
    }
}
