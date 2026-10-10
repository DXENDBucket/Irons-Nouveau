package dev.ironsnouveau.platform;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import dev.ironsnouveau.glyph.NativeFormAugment;
import java.util.List;

public final class GlyphHints {
    private GlyphHints() {}
    public static void carrier(AbstractSpellPart glyph, List<NativeFormAugment> forms) {
        forms.forEach(form -> glyph.augmentDescriptions.put(form,
                net.minecraft.network.chat.Component.translatable("irons_nouveau.augment.native_form")));
        glyph.augmentDescriptions.put(AugmentAmplify.INSTANCE, net.minecraft.network.chat.Component.translatable("irons_nouveau.augment.native_level"));
        glyph.augmentDescriptions.put(AugmentDampen.INSTANCE, net.minecraft.network.chat.Component.translatable("irons_nouveau.augment.native_level"));
    }
}
