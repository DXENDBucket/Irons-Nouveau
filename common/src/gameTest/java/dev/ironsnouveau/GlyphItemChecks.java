package dev.ironsnouveau;

import com.google.gson.JsonParser;
import dev.ironsnouveau.compat.NativeGlyphResources;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import java.util.function.Function;
import java.util.stream.Stream;

public final class GlyphItemChecks {
    public static void models(GameTestHelper h) {
        var textures=NativeGlyphResources.textures();
        for(var part:Stream.concat(GlyphCatalog.glyphs().stream(),GlyphCatalog.forms().stream()).toList()) {
            var model=dev.ironsnouveau.platform.Locations.id("irons_nouveau:item/"+part.getRegistryName().getPath());
            var json=JsonParser.parseString(NativeGlyphResources.model(model)).getAsJsonObject();
            var icon=dev.ironsnouveau.glyph.GlyphIcons.source(part).texture();
            h.assertTrue(textures.contains(icon),"Every item model icon is also registered for atlas loading");
            h.assertTrue(json.get("parent").getAsString().equals("minecraft:item/generated")
                    &&json.getAsJsonObject("textures").get("layer0").getAsString().equals(NativeGlyphResources.sprite(icon).toString()),"Held model uses native spell icon: "+part.getRegistryName());
            var fallback=NativeGlyphResources.model(model,texture->false);
            h.assertTrue(fallback.contains("ars_nouveau:item/glyph_template"),"Missing upstream icons have a valid paper fallback");
        }
        h.assertTrue(NativeGlyphResources.model(dev.ironsnouveau.platform.Locations.id("minecraft:item/stick"))==null,"Other namespaces' item models are untouched");h.succeed();
    }
    public static void scrolls(GameTestHelper h,Function<ResourceLocation,Ingredient> factory) {
        for(var id:java.util.List.of("irons_spellbooks:firebolt","irons_spellbooks:starfall","irons_spellbooks:heal")) {
            var spellId=dev.ironsnouveau.platform.Locations.id(id);var spell=SpellRegistry.getSpell(spellId);var ingredient=factory.apply(spellId);
            var examples=ingredient.getItems();int min=Math.max(1,spell.getMinLevel()),max=spell.getMaxLevel();
            h.assertTrue(examples.length==max-min+1,"JEI ingredient includes every native scroll level: "+id);
            for(int level=min;level<=max;level++) {
                int expected=level;
                h.assertTrue(java.util.Arrays.stream(examples).anyMatch(s->ISpellContainer.get(s).getSpellAtIndex(0).getLevel()==expected),"Higher-level scroll is indexed for uses: "+id+"/"+level);
                h.assertTrue(ingredient.test(examples[level-min]),"Every displayed level really crafts");
            }
            var above=new ItemStack(ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(spell,max+1,above);
            h.assertTrue(ingredient.test(above),"Existing acceptance of positive custom levels is retained");
            var wrong=new ItemStack(ItemRegistry.SCROLL.get());ISpellContainer.createScrollContainer(SpellRegistry.MAGIC_MISSILE_SPELL.get(),1,wrong);
            h.assertTrue(!ingredient.test(wrong),"A different spell never substitutes for the requested scroll");
        }
        h.succeed();
    }
}
