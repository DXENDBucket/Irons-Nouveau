package dev.ironsnouveau.api;

import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import net.minecraft.resources.ResourceLocation;
import java.util.Objects;

public record GlyphDefinition(ResourceLocation glyphId, ResourceLocation spellId, String name,
                              int level, int manaCost, SpellTier tier, SpellSchool school,
                              boolean harmful, boolean supportsDuration, SpellAdapter adapter) {
    public GlyphDefinition {
        Objects.requireNonNull(glyphId); Objects.requireNonNull(spellId); Objects.requireNonNull(name);
        Objects.requireNonNull(tier); Objects.requireNonNull(school); Objects.requireNonNull(adapter);
        if (level < 1 || manaCost < 0) throw new IllegalArgumentException("Invalid level or mana cost");
    }
}
