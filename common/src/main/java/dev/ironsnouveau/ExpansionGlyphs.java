package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.bridge.*;

final class ExpansionGlyphs {
    private ExpansionGlyphs() {}
    static void register() {
        add("blood_step", "Blood Step", dev.ironsnouveau.platform.GlyphSchools.NECROMANCY, false, true, EntitySelectionAdapters.BLOOD_STEP);
        add("frost_step", "Frost Step", SpellSchools.ELEMENTAL_WATER, false, true, EntitySelectionAdapters.FROST_STEP);
        add("telekinesis", "Telekinesis", dev.ironsnouveau.platform.GlyphSchools.NECROMANCY, true, true, EntitySelectionAdapters.TELEKINESIS);
        add("starfall", "Starfall", SpellSchools.MANIPULATION, true, true, EmissionAdapters.STARFALL);
        add("arrow_volley", "Arrow Volley", SpellSchools.CONJURATION, true, false, EmissionAdapters.ARROWS);
        add("chain_creeper", "Chain Creeper", SpellSchools.CONJURATION, true, false, EmissionAdapters.CREEPERS);
        add("fang_swirl", "Fang Swirl", SpellSchools.CONJURATION, true, true, EmissionAdapters.FANG_SWIRL);
        add("blaze_storm", "Blaze Storm", SpellSchools.ELEMENTAL_FIRE, true, true, EmissionAdapters.BLAZE_STORM);
        add("cloud_of_regeneration", "Cloud of Regeneration", SpellSchools.ABJURATION, false, true, EmissionAdapters.CLOUD);
        add("flaming_strike", "Flaming Strike", SpellSchools.ELEMENTAL_FIRE, true, false, TerrainAdapters.FLAMING_STRIKE);
        add("raise_hell", "Raise Hell", SpellSchools.ELEMENTAL_FIRE, true, false, TerrainAdapters.RAISE_HELL);
        add("ice_spikes", "Ice Spikes", SpellSchools.ELEMENTAL_WATER, true, false, TerrainAdapters.ICE_SPIKES);
        add("firecracker", "Firecracker", SpellSchools.CONJURATION, true, false, TerrainAdapters.FIRECRACKER);
        add("spectral_hammer", "Spectral Hammer", SpellSchools.CONJURATION, false, false, TerrainAdapters.HAMMER);
        add("touch_dig", "Touch Dig", SpellSchools.ELEMENTAL_EARTH, false, false, TerrainAdapters.TOUCH_DIG);
        add("shadow_slash", "Shadow Slash", SpellSchools.MANIPULATION, false, false, MotionAdapters.SHADOW_SLASH);
        add("burning_dash", "Burning Dash", SpellSchools.ELEMENTAL_FIRE, false, false, MotionAdapters.BURNING_DASH);
        add("ascension", "Ascension", SpellSchools.ELEMENTAL_AIR, false, false, MotionAdapters.ASCENSION);
        add("volt_strike", "Volt Strike", SpellSchools.ELEMENTAL_AIR, false, false, MotionAdapters.VOLT_STRIKE);
    }
    private static void add(String id, String name, SpellSchool school, boolean harmful, boolean duration, LocationSpellAdapter adapter) {
        IronsNouveau.add(id, name, 0, SpellTier.ONE, school, harmful, duration, adapter);
    }
}
