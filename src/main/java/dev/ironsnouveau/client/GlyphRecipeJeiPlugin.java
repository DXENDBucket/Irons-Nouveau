package dev.ironsnouveau.client;

import com.hollingsworth.arsnouveau.client.jei.JEIArsNouveauPlugin;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import dev.ironsnouveau.IronsNouveau;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.progression.SpellProgress;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import java.util.List;

/** Optional JEI entry point; never referenced by common startup or server code. */
@JeiPlugin
public final class GlyphRecipeJeiPlugin implements IModPlugin {
    private IJeiRuntime runtime;
    private Boolean previous;
    private List<RecipeHolder<GlyphRecipe>> hidden = List.of();

    public GlyphRecipeJeiPlugin() { NeoForge.EVENT_BUS.addListener(this::tick); }

    @Override public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(IronsNouveau.MOD_ID, "glyph_learning");
    }

    @Override public void onRuntimeAvailable(IJeiRuntime runtime) {
        this.runtime = runtime;
        previous = null;
        hidden = List.of();
        refresh();
    }

    private void tick(ClientTickEvent.Post event) { refresh(); }

    private void refresh() {
        if (runtime == null) return;
        boolean hide = SpellLevelConfig.requiresCrafting();
        if (previous != null && previous == hide) return;
        var manager = runtime.getRecipeManager();
        var type = JEIArsNouveauPlugin.GLYPH_RECIPE_TYPE.get();
        if (hide) {
            // Only restore recipes that this plugin actually hid, not pre-existing hidden recipes.
            hidden = manager.createRecipeLookup(type).get()
                    .filter(recipe -> SpellProgress.spellId(recipe.value().getSpellPart()) != null)
                    .toList();
            manager.hideRecipes(type, hidden);
        } else if (!hidden.isEmpty()) {
            manager.unhideRecipes(type, hidden);
            hidden = List.of();
        }
        previous = hide;
    }

    @Override public void onRuntimeUnavailable() {
        runtime = null;
        previous = null;
        hidden = List.of();
    }
}
