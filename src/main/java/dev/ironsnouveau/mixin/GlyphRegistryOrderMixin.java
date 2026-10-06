package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.crafting.recipes.GlyphRecipe;
import com.hollingsworth.arsnouveau.setup.registry.CreativeTabRegistry;
import dev.ironsnouveau.glyph.GlyphDisplayOrder;
import java.util.Comparator;
import java.util.function.Function;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CreativeTabRegistry.class, remap = false)
public abstract class GlyphRegistryOrderMixin {
    @Shadow public static Comparator<AbstractSpellPart> COMPARE_GLYPH_BY_TYPE;
    @Shadow public static Comparator<AbstractSpellPart> COMPARE_TYPE_THEN_NAME;
    @Shadow public static Comparator<AbstractSpellPart> COMPARE_TIER_THEN_NAME;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void ironsNouveau$afterPeers(CallbackInfo ci) {
        Function<AbstractSpellPart, AbstractSpellPart> recipePart = Function.identity();
        COMPARE_GLYPH_BY_TYPE = GlyphDisplayOrder.ironLast(COMPARE_GLYPH_BY_TYPE, recipePart, AbstractSpellPart::getTypeIndex);
        COMPARE_TYPE_THEN_NAME = GlyphDisplayOrder.ironLast(COMPARE_TYPE_THEN_NAME, recipePart, AbstractSpellPart::getTypeIndex);
        COMPARE_TIER_THEN_NAME = GlyphDisplayOrder.ironLast(COMPARE_TIER_THEN_NAME, recipePart, AbstractSpellPart::getTypeIndex);
    }
}
