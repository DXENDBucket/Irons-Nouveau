package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.common.block.tile.ScribesTile;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.progression.SpellProgress;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A client recipe request cannot bypass the server's independent learning policy. */
@Mixin(value = ScribesTile.class, remap = false)
public abstract class ScribeLearningMixin {
    @Inject(method = "setRecipe", at = @At("HEAD"), cancellable = true)
    private void ironsNouveau$learningPolicy(GlyphRecipe recipe, Player player, CallbackInfo ci) {
        if (recipe == null) return;
        var glyph = recipe.getSpellPart();
        var spell = SpellProgress.spellId(glyph);
        if (spell != null && (SpellLevelConfig.requiresCrafting()
                || !GlyphAccessEvent.allowed(player, glyph.getRegistryName(), spell,
                    SpellProgress.baseLevel(player, spell), GlyphAccessEvent.Action.LEARN))) ci.cancel();
    }
}
