package dev.ironsnouveau.mixin;
import com.google.gson.JsonElement;
import dev.ironsnouveau.compat.NativeGlyphResources;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Map;

@Mixin(RecipeManager.class)
public abstract class NativeGlyphRecipeMixin {
    @Inject(method="apply",at=@At("HEAD"))
    private void ironsNouveau$nativeRecipes(Map<ResourceLocation,JsonElement> recipes,ResourceManager resources,ProfilerFiller profiler,CallbackInfo ci) {
        NativeGlyphResources.recipes(recipes);
    }
}
