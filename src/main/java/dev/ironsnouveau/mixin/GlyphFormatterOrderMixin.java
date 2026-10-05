package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.client.gui.book.GlyphFormatter;
import dev.ironsnouveau.glyph.DisplayGroupOrder;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import java.util.Comparator;
import java.util.List;
import java.util.IdentityHashMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = GlyphFormatter.class, remap = false)
public abstract class GlyphFormatterOrderMixin {
    @WrapOperation(method = "buildSortedWidgets", at = @At(value = "INVOKE", target = "Ljava/util/List;sort(Ljava/util/Comparator;)V"))
    private void ironsNouveau$afterPeers(List<AbstractSpellPart> glyphs, Comparator<AbstractSpellPart> comparator, Operation<Void> original) {
        var categories = List.copyOf(GlyphFormatter.CATEGORIES);
        var groups = new IdentityHashMap<AbstractSpellPart, Integer>();
        for (var glyph : glyphs) {
            int group = categories.size() + glyph.getTypeIndex();
            // Later addon categories specialize broad predicates such as AbstractEffect.
            // Use the actual registered predicate, without linking against optional addon classes.
            for (int i = 0; i < categories.size(); i++)
                if (categories.get(i).filter().test(glyph)) group = i;
            groups.put(glyph, group);
        }
        original.call(glyphs, DisplayGroupOrder.comparator(glyphs, comparator, groups::get,
                part -> part instanceof AbstractAugment ? 3 : part.getTypeIndex(), group -> group,
                part -> part.getRegistryName().getNamespace().equals("irons_nouveau")));
    }
}
