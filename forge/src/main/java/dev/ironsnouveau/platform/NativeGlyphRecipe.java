package dev.ironsnouveau.platform;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
public final class NativeGlyphRecipe {
    private NativeGlyphRecipe() {}
    public static void populate(JsonObject json,JsonObject scroll,ResourceLocation output) {
        var inputs=new JsonArray();var scrollEntry=new JsonObject();scrollEntry.add("item",scroll);inputs.add(scrollEntry);
        var gem=new JsonObject();gem.addProperty("item","ars_nouveau:source_gem");var gemEntry=new JsonObject();gemEntry.add("item",gem);inputs.add(gemEntry);
        json.add("inputItems",inputs);json.addProperty("output",output.toString());json.addProperty("count",1);
    }
}
