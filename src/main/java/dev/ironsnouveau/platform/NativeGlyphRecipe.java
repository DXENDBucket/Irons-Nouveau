package dev.ironsnouveau.platform;
import com.google.gson.*;
import net.minecraft.resources.ResourceLocation;
public final class NativeGlyphRecipe {
    private NativeGlyphRecipe() {}
    public static void populate(JsonObject json,JsonObject scroll,ResourceLocation output) {
        var inputs=new JsonArray();inputs.add(scroll);var gem=new JsonObject();gem.addProperty("item","ars_nouveau:source_gem");inputs.add(gem);
        json.add("inputs",inputs);var result=new JsonObject();result.addProperty("id",output.toString());result.addProperty("count",1);json.add("output",result);
    }
}
