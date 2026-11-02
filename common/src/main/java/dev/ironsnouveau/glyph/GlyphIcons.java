package dev.ironsnouveau.glyph;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import dev.arsconflux.api.glyph.GlyphIcon;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.resources.ResourceLocation;
/** Native spell-icon references; no texture extraction, scroll model or loader-specific rendering. */
public final class GlyphIcons {
    public static GlyphIcon source(AbstractSpellPart part) {
        var ability=dev.ironsnouveau.ability.IronAbility.of(part);
        if(ability!=null)return GlyphIcon.texture(dev.ironsnouveau.platform.Locations.id("irons_spellbooks:textures/item/"+ability.icon+".png"));
        ResourceLocation id=part instanceof AbstractBridgeGlyph?((AbstractBridgeGlyph)part).definition().spellId()
                :part instanceof NativeFormAugment?((NativeFormAugment)part).spellId():null;
        if(id==null)return null;var spell=SpellRegistry.getSpell(id);
        return spell==SpellRegistry.none()?null:GlyphIcon.texture(spell.getSpellIconResource());
    }
    private GlyphIcons(){}
}
