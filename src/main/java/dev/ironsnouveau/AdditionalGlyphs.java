package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import dev.ironsnouveau.bridge.*;

/** Additional adapters use the same registration, mastery, icons and trigger billing as the original set. */
final class AdditionalGlyphs {
    private AdditionalGlyphs() {}
    static void register() {
        IronsNouveau.add("heartstop", "Heartstop", 0, SpellTier.ONE, SpellSchools.CONJURATION, false, true, AdditionalTargetAdapters.HEARTSTOP);
        IronsNouveau.add("abyssal_shroud", "Abyssal Shroud", 0, SpellTier.ONE, SpellSchools.CONJURATION, false, true, AdditionalTargetAdapters.SHROUD);
        IronsNouveau.add("planar_sight", "Planar Sight", 0, SpellTier.ONE, SpellSchools.MANIPULATION, false, true, AdditionalTargetAdapters.SIGHT);
        IronsNouveau.add("invisibility", "Invisibility", 0, SpellTier.ONE, SpellSchools.CONJURATION, false, true, AdditionalTargetAdapters.INVISIBILITY);
        IronsNouveau.add("angel_wing", "Angel Wings", 0, SpellTier.ONE, SpellSchools.ABJURATION, false, true, AdditionalTargetAdapters.WINGS);
        IronsNouveau.add("frostbite", "Frostbite", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_WATER, false, true, AdditionalTargetAdapters.FROSTBITE);
        IronsNouveau.add("charge", "Charge", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_AIR, false, true, AdditionalTargetAdapters.CHARGE);
        IronsNouveau.add("gluttony", "Gluttony", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_EARTH, false, true, AdditionalTargetAdapters.GLUTTONY);
        IronsNouveau.add("spider_aspect", "Spider Aspect", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_EARTH, false, true, AdditionalTargetAdapters.SPIDER);
        IronsNouveau.add("echoing_strikes", "Echoing Strikes", 0, SpellTier.ONE, SpellSchools.MANIPULATION, false, true, AdditionalTargetAdapters.ECHO);
        IronsNouveau.add("blessing_of_life", "Blessing of Life", 0, SpellTier.ONE, SpellSchools.ABJURATION, false, false, NativeAdapters.HEAL);
        IronsNouveau.add("wololo", "Wololo", 0, SpellTier.ONE, SpellSchools.CONJURATION, false, false, AdditionalTargetAdapters.WOLOLO);
        IronsNouveau.add("sacrifice", "Sacrifice", 0, SpellTier.ONE, SpellSchools.CONJURATION, false, false, AdditionalTargetAdapters.SACRIFICE);
        IronsNouveau.add("devour", "Devour", 0, SpellTier.ONE, SpellSchools.CONJURATION, true, false, AdditionalTargetAdapters.DEVOUR);
        IronsNouveau.add("acupuncture", "Acupuncture", 0, SpellTier.ONE, SpellSchools.CONJURATION, true, false, AdditionalTargetAdapters.ACUPUNCTURE);
        IronsNouveau.add("counterspell", "Counterspell", 0, SpellTier.ONE, SpellSchools.MANIPULATION, true, false, AdditionalTargetAdapters.COUNTERSPELL);
        IronsNouveau.add("ice_tomb", "Ice Tomb", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_WATER, false, true, AdditionalTargetAdapters.ICE_TOMB);
        IronsNouveau.add("earthquake", "Earthquake", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_EARTH, true, true, AreaSpellAdapters.EARTHQUAKE);
        IronsNouveau.add("blizzard", "Blizzard", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_WATER, true, true, AreaSpellAdapters.BLIZZARD);
        IronsNouveau.add("poison_splash", "Poison Splash", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_EARTH, true, true, AreaSpellAdapters.POISON_SPLASH);
        IronsNouveau.add("gravity_fissure", "Gravity Fissure", 0, SpellTier.ONE, SpellSchools.MANIPULATION, true, true, AreaSpellAdapters.GRAVITY_FISSURE);
        IronsNouveau.add("scorch", "Scorch", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_FIRE, true, true, AreaSpellAdapters.SCORCH);
        IronsNouveau.add("heat_surge", "Heat Surge", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_FIRE, true, true, AreaSpellAdapters.HEAT_SURGE);
        IronsNouveau.add("frostwave", "Frostwave", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_WATER, true, true, AreaSpellAdapters.FROSTWAVE);
        IronsNouveau.add("shockwave", "Shockwave", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_AIR, true, false, AreaSpellAdapters.SHOCKWAVE);
        IronsNouveau.add("ice_block", "Ice Block", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_WATER, true, false, PointEntityAdapters.ICE_BLOCK);
        IronsNouveau.add("shield", "Shield", 0, SpellTier.ONE, SpellSchools.CONJURATION, false, true, PointEntityAdapters.SHIELD);
        IronsNouveau.add("scapegoat", "Scapegoat", 0, SpellTier.ONE, SpellSchools.CONJURATION, false, true, PointEntityAdapters.SCAPEGOAT);
        IronsNouveau.add("stomp", "Stomp", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_EARTH, true, false, PointEntityAdapters.STOMP);
        IronsNouveau.add("fang_strike", "Fang Strike", 0, SpellTier.ONE, SpellSchools.CONJURATION, true, false, PointEntityAdapters.FANG_STRIKE);
        IronsNouveau.add("fang_ward", "Fang Ward", 0, SpellTier.ONE, SpellSchools.CONJURATION, true, false, PointEntityAdapters.FANG_WARD);
        IronsNouveau.add("sonic_boom", "Sonic Boom", 0, SpellTier.ONE, SpellSchools.CONJURATION, true, false, PointEntityAdapters.SONIC_BOOM);
        IronsNouveau.add("divine_smite", "Divine Smite", 0, SpellTier.ONE, SpellSchools.ABJURATION, true, false, PointEntityAdapters.DIVINE_SMITE);
        IronsNouveau.add("summon_ender_chest", "Summon Ender Chest", 0, SpellTier.ONE, SpellSchools.MANIPULATION, false, false, PointEntityAdapters.ENDER_CHEST);
    }
}
