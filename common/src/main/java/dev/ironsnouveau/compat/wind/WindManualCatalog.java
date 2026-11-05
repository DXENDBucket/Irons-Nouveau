package dev.ironsnouveau.compat.wind;

import com.hollingsworth.arsnouveau.api.spell.*;
import dev.ironsnouveau.GlyphCatalog;
import dev.ironsnouveau.api.GlyphDefinition;
import dev.ironsnouveau.compat.NativeSpellIndex;
import dev.ironsnouveau.glyph.NativeFormAugment;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;

/** Registration identities are safe to load with Wind absent. Native implementation loads on demand. */
public final class WindManualCatalog {
    public static final Set<String> SPELLS=Set.of("wind_blade","tornado","almighty_push","tailwind","wind_jump","iron_slash","aeropic");
    public static boolean owns(ResourceLocation spell) {
        return spell!=null&&spell.getNamespace().equals("wind_spellbooks")&&SPELLS.contains(spell.getPath());
    }
    public static AbstractSpellPart register(ResourceLocation spell) {
        if(!owns(spell))return null;
        return Loaded.register(spell);
    }
    private static final class Loaded {
        static AbstractSpellPart register(ResourceLocation spell) {
            var id=NativeSpellIndex.glyphId(spell);
            if(spell.getPath().equals("wind_blade")) {
                var glyph=new NativeFormAugment(id,spell,"Wind Blade",SpellSchools.ELEMENTAL_AIR,new WindBladeAdapter(),0,SpellTier.ONE);
                GlyphCatalog.addForm(glyph);return glyph;
            }
            boolean area=Set.of("tornado","almighty_push").contains(spell.getPath());
            return GlyphCatalog.addManual(new GlyphDefinition(id,spell,spell.getPath(),1,0,SpellTier.ONE,
                    SpellSchools.ELEMENTAL_AIR,area,area||Set.of("tailwind","wind_jump").contains(spell.getPath()),WindAdapters.forSpell(spell.getPath())));
        }
    }
    private WindManualCatalog(){}
}
