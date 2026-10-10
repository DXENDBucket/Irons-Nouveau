package dev.ironsnouveau.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import dev.ironsnouveau.progression.SpellProgress;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import java.util.Comparator;
import java.util.function.Function;
import java.util.function.ToIntFunction;

/** Presentation only: category, source, native Iron school, then upstream ordering. */
public final class GlyphDisplayOrder {
    private GlyphDisplayOrder() {}

    public static <T> Comparator<T> ironLast(Comparator<T> original, Function<T, AbstractSpellPart> part,
                                            ToIntFunction<AbstractSpellPart> category) {
        return Comparator.<T>comparingInt(value -> category.applyAsInt(part.apply(value)))
                .thenComparingInt(value -> part.apply(value).getRegistryName().getNamespace().equals("irons_nouveau") ? 1 : 0)
                .thenComparing(value -> schoolKey(part.apply(value)))
                .thenComparing(original);
    }

    /** Use the native Iron school, not Ars' broader mapped schools. Non-Iron ordering is unchanged. */
    public static String schoolKey(AbstractSpellPart part) {
        var id = SpellProgress.spellId(part);
        if (id == null) return "";
        var spell = SpellRegistry.getSpell(id);
        if (spell == SpellRegistry.none() || spell.getSchoolType() == null) return "";
        return spell.getSchoolType().getId().toString();
    }

    public static Comparator<AbstractSpellPart> bySchool(Comparator<AbstractSpellPart> original) {
        return Comparator.comparing(GlyphDisplayOrder::schoolKey).thenComparing(original);
    }
}
