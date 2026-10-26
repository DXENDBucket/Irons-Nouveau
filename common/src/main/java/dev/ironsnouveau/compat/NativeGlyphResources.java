package dev.ironsnouveau.compat;

import com.google.gson.*;
import dev.ironsnouveau.GlyphCatalog;
import dev.ironsnouveau.glyph.AbstractBridgeGlyph;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.resources.ResourceLocation;
import java.util.Map;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Stream;

/** Generated resources reference upstream names/textures; no upstream assets are copied. */
public final class NativeGlyphResources {
    private NativeGlyphResources() {}
    public static AbstractBridgeGlyph glyph(ResourceLocation id) {
        return GlyphCatalog.glyphs().stream().filter(g->g.nativeSelf()&&g.getRegistryName().equals(id)).findFirst().orElse(null);
    }
    public static String model(ResourceLocation model) {
        return model(model,texture->true);
    }
    public static String model(ResourceLocation model,Predicate<ResourceLocation> available) {
        if(!model.getNamespace().equals("irons_nouveau")||!model.getPath().startsWith("item/glyph_"))return null;
        var part=com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.getSpellPart(
                dev.ironsnouveau.platform.Locations.id(model.getNamespace(),model.getPath().substring(5)));
        var id=dev.ironsnouveau.casting.BoundSpellSupport.ironSpell(part);if(id==null)return null;
        var spell=SpellRegistry.getSpell(id);if(spell==SpellRegistry.none())return null;
        var texture=spell.getSpellIconResource();
        if(!available.test(texture))texture=dev.ironsnouveau.platform.Locations.id("ars_nouveau:textures/item/glyph_template.png");
        return "{\"parent\":\"minecraft:item/generated\",\"textures\":{\"layer0\":\""+sprite(texture)+"\"}}";
    }
    public static ResourceLocation sprite(ResourceLocation texture) {
        String path=texture.getPath();
        if(path.startsWith("textures/"))path=path.substring(9);
        if(path.endsWith(".png"))path=path.substring(0,path.length()-4);
        return dev.ironsnouveau.platform.Locations.id(texture.getNamespace(),path);
    }
    public static List<ResourceLocation> textures() {
        return Stream.concat(GlyphCatalog.glyphs().stream(),GlyphCatalog.forms().stream())
                .map(dev.ironsnouveau.glyph.GlyphIcons::source).filter(java.util.Objects::nonNull)
                .map(dev.arsconflux.api.glyph.GlyphIcon::texture).filter(java.util.Objects::nonNull).distinct().toList();
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
