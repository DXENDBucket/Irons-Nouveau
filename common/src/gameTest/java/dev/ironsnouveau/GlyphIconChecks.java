package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm;
import dev.arsconflux.api.glyph.*;
import dev.ironsnouveau.glyph.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import java.util.stream.Stream;

public final class GlyphIconChecks {
    public static void nativeIconsAreDeclaredWithoutClientClasses(GameTestHelper h) {
        var parts=Stream.concat(GlyphCatalog.glyphs().stream(), GlyphCatalog.forms().stream()).toList();
        h.assertTrue(parts.size()>=100,"Full Iron catalog registered");
        for(var part:parts) {
            h.assertTrue(GlyphVisuals.applies(part),"Iron opts into visuals: "+part.getRegistryName());
            var icon=GlyphVisuals.icon(part);
            var id=part instanceof AbstractBridgeGlyph bridge?bridge.definition().spellId():((NativeFormAugment)part).spellId();
            h.assertTrue(icon!=null && icon.item()==null && icon.texture().equals(SpellRegistry.getSpell(id).getSpellIconResource()),
                "Native spell texture reference: "+id);
        }
        h.assertTrue(!GlyphVisuals.applies(EffectHarm.INSTANCE) && GlyphVisuals.icon(EffectHarm.INSTANCE)==null,"Native Ars glyphs pass through");
        var source=parts.get(0).getGlyph().getDefaultInstance();
        var icon=GlyphIcon.item(source);source.setCount(0);
        h.assertTrue(!icon.item().isEmpty(),"Item icon owns a snapshot");
        boolean rejected=false;try{GlyphIcon.texture(icon.item().getItem().builtInRegistryHolder().key().location()).placed(Double.NaN,0,0);}catch(IllegalArgumentException e){rejected=true;}
        h.assertTrue(rejected,"Invalid placement rejected");
        h.succeed();
    }
    private GlyphIconChecks(){}
}
