package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityOrbitProjectile;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectOrbit;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import net.minecraft.resources.ResourceLocation;
import java.util.*;

/** Explicit carrier capabilities; optional addons are discovered by glyph ID, without hard class links. */
public enum CarrierProfiles {
    STRAIGHT("ars_nouveau:glyph_projectile", false),
    ORBIT("ars_nouveau:glyph_orbit", true),
    ARC("ars_elemental:glyph_arc_projectile", true),
    HOMING("ars_elemental:glyph_homing_projectile", true),
    SPLASH("arsomega:glyph_missile", true),
    TRAIL("not_enough_glyphs:glyph_trail", true);

    private final ResourceLocation glyphId;
    private final boolean driven;
    private Set<AbstractAugment> originalAugments = Set.of();
    CarrierProfiles(String glyphId, boolean driven) { this.glyphId = ResourceLocation.parse(glyphId); this.driven = driven; }
    public boolean driven() { return driven; }
    public AbstractSpellPart glyph() { return GlyphRegistry.getSpellPart(glyphId); }
    public void rememberOriginalAugments(AbstractSpellPart glyph) { originalAugments = Set.copyOf(glyph.compatibleAugments); }
    public boolean originallySupported(AbstractAugment augment) { return originalAugments.contains(augment); }
    public static CarrierProfiles of(AbstractSpellPart action) {
        if (action == null) return null;
        if (action == MethodProjectile.INSTANCE) return STRAIGHT;
        if (action == EffectOrbit.INSTANCE) return ORBIT;
        for (var profile : values()) if (profile.glyphId.equals(action.getRegistryName())) return profile;
        return null;
    }
    public static CarrierProfiles of(EntityProjectileSpell carrier) {
        return carrier instanceof EntityOrbitProjectile ? ORBIT : of(carrier.resolver().castType);
    }
    public Set<AbstractAugment> augments() {
        var result = new HashSet<AbstractAugment>(Set.of(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE,
                AugmentAccelerate.INSTANCE, AugmentDecelerate.INSTANCE, AugmentSplit.INSTANCE));
        if (driven) result.addAll(Set.of(AugmentPierce.INSTANCE, AugmentSensitive.INSTANCE));
        if (this == ORBIT || this == SPLASH || this == TRAIL) result.addAll(Set.of(AugmentAOE.INSTANCE, AugmentExtendTime.INSTANCE, AugmentDurationDown.INSTANCE));
        return Set.copyOf(result);
    }
}
