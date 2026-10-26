package dev.ironsnouveau.compat;

import com.google.gson.*;
import dev.ironsnouveau.GlyphCatalog;
import dev.ironsnouveau.glyph.AbstractBridgeGlyph;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;

/** Generated resources reference upstream names/textures; no upstream assets are copied. */
public final class NativeGlyphResources {
    private NativeGlyphResources() {}
    public static AbstractBridgeGlyph glyph(ResourceLocation id) {
        return GlyphCatalog.glyphs().stream().filter(g->g.nativeSelf()&&g.getRegistryName().equals(id)).findFirst().orElse(null);
    }
    public static String model(ResourceLocation model) {
        if(!model.getNamespace().equals("irons_nouveau")||!model.getPath().startsWith("item/glyph_native/"))return null;
        var glyph=glyph(dev.ironsnouveau.platform.Locations.id(model.getNamespace(),model.getPath().substring(5)));
        if(glyph==null)return null;
        var spell=SpellRegistry.getSpell(glyph.definition().spellId());if(spell==SpellRegistry.none())return null;
        var texture=spell.getSpellIconResource();String path=texture.getPath();
        if(path.startsWith("textures/"))path=path.substring(9);
        if(path.endsWith(".png"))path=path.substring(0,path.length()-4);
        return "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\""+texture.getNamespace()+":"+path+"\"}}";
    }
    public static void recipes(Map<ResourceLocation,JsonElement> recipes) {
        for(var glyph:GlyphCatalog.glyphs())if(glyph.nativeSelf()) {
            var spell=SpellRegistry.getSpell(glyph.definition().spellId());if(spell==SpellRegistry.none()||!spell.isEnabled())continue;
            var json=new JsonObject();json.addProperty("type","ars_nouveau:glyph");json.addProperty("exp",27);
            var scroll=new JsonObject();scroll.addProperty("type","irons_nouveau:spell_scroll");scroll.addProperty("spell",glyph.definition().spellId().toString());
            dev.ironsnouveau.platform.NativeGlyphRecipe.populate(json,scroll,glyph.getRegistryName());
            recipes.putIfAbsent(glyph.getRegistryName(),json);
        }
    }
}
