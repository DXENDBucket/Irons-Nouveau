package dev.ironsnouveau.ability;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import dev.ironsnouveau.platform.Locations;
import net.minecraft.resources.ResourceLocation;

/** Item/Boss abilities have no native scroll, spell level or scroll-crafting prerequisite. */
public enum IronAbility {
    UNDEAD_RIFT("undead_rift", "Undead Rift", 300, 40, 300, 0, "blood_vial"),
    FIERY_DAGGER("fiery_dagger", "Fiery Dagger", 35, 0, 0, 0, "cinder_essence"),
    DAGGER_ZONE("dagger_zone", "Dagger Zone", 180, 20, 100, 20, "fire_rune"),
    RING_OF_FIRE("ring_of_fire", "Ring of Fire", 100, 20, 160, 15, "divine_soulshard"),
    EXPULSION("expulsion", "Expulsion", 25, 0, 40, 0, "expulsion_ring"),
    OMINOUS_FIRE_ORB("ominous_fire_orb", "Ominous Fire Orb", 600, 40, 500, 0, "divine_soulshard");
    public final String path, name, icon;
    public final int mana, chant, cooldown, pulse;
    IronAbility(String path,String name,int mana,int chant,int cooldown,int pulse,String icon) {
        this.path=path;this.name=name;this.mana=mana;this.chant=chant;this.cooldown=cooldown;this.pulse=pulse;this.icon=icon;
    }
    public ResourceLocation id(){return Locations.id("irons_nouveau",path);}
    public ResourceLocation glyphId(){return Locations.id("irons_nouveau","glyph_"+path);}
    public static IronAbility of(ResourceLocation id){for(var a:values())if(a.id().equals(id))return a;return null;}
    public static IronAbility of(AbstractSpellPart part){
        return part instanceof AbilityGlyph g?g.ability:part instanceof AbilityProjectile g?g.ability:null;
    }
    public boolean enabled(){var p=com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.getSpellPart(glyphId());return p!=null&&p.isEnabled();}
}
