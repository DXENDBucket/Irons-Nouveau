package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.projectile.CarrierProfile;
import dev.arsconflux.api.projectile.CarrierRegistry;
import java.util.Set;

/** Compatibility names for existing consumers. All declarations and lookup policy live in Conflux.
 * New carriers register with CarrierRegistry and do not require extending this legacy enum. */
public enum CarrierProfiles {
    STRAIGHT("ars_nouveau:glyph_projectile"), ORBIT("ars_nouveau:glyph_orbit"),
    ARC("ars_elemental:glyph_arc_projectile"), HOMING("ars_elemental:glyph_homing_projectile"),
    SPLASH("arsomega:glyph_missile"), TRAIL("not_enough_glyphs:glyph_trail"),
    PROPAGATE_STRAIGHT("arsomega:glyph_propagate_projectile"), PROPAGATE_ARC("ars_elemental:glyph_propagator_arc"),
    PROPAGATE_HOMING("ars_elemental:glyph_propagator_homing"), PROPAGATE_SPLASH("arsomega:glyph_propagate_missile");
    private final String id;
    CarrierProfiles(String id) { this.id = id; }
    public CarrierProfile core() { return CarrierRegistry.get(CarrierRegistry.id(id)); }
    public boolean driven() { return core().arsDriven(); }
    public boolean propagator() { return core().propagator(); }
    public AbstractSpellPart glyph() { return core().glyph(); }
    public void rememberOriginalAugments(AbstractSpellPart glyph) { core().rememberOriginalAugments(glyph); }
    public boolean originallySupported(AbstractAugment augment) { return core().originallySupported(augment); }
    public Set<AbstractAugment> augments() { return core().augments(); }
    private static CarrierProfiles legacy(CarrierProfile profile) {
        if (profile != null) for (var value : values()) if (value.core() == profile) return value;
        return null;
    }
    public static CarrierProfiles of(AbstractSpellPart part) { return legacy(CarrierRegistry.of(part)); }
    public static CarrierProfiles of(EntityProjectileSpell projectile) { return legacy(CarrierRegistry.of(projectile)); }
}
