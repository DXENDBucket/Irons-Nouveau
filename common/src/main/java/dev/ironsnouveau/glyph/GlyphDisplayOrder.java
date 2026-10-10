package dev.ironsnouveau.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import dev.ironsnouveau.progression.SpellProgress;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import java.util.Comparator;

/** Provider-specific native school key; shared cross-provider sorting belongs to Conflux. */
public final class GlyphDisplayOrder {
    private GlyphDisplayOrder() {}

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
