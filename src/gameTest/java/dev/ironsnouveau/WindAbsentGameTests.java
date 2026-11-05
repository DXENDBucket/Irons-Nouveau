package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("irons_nouveau_wind_absent") @PrefixGameTestTemplate(false)
public final class WindAbsentGameTests {
    @GameTest(template="empty") public static void optional(GameTestHelper h){
        for(var name:dev.ironsnouveau.compat.wind.WindManualCatalog.SPELLS)
            h.assertTrue(com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.getSpellPart(
                    dev.ironsnouveau.compat.NativeSpellIndex.glyphId(dev.ironsnouveau.platform.Locations.id("wind_spellbooks",name)))==null,
                    "Absent optional mod registers no orphan glyph: "+name);
        h.assertTrue(!IronsNouveau.forms().isEmpty()&&!IronsNouveau.glyphs().isEmpty(),"Iron's own glyphs still register");h.succeed();
    }
}
