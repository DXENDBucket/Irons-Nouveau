package dev.ironsnouveau.glyph;

import dev.ironsnouveau.api.GlyphDefinition;
import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import net.minecraft.network.chat.Component;
import java.util.Map;

/** Ars 5 augment descriptions; effect execution is shared with Ars 4. */
public final class BridgeGlyph extends AbstractBridgeGlyph {
    public BridgeGlyph(GlyphDefinition definition) {
        super(definition);
        // Super calls virtual methods before definition is assigned. Populate translated hints here.
        for (var augment : compatibleAugments) {
            String key = augment == AugmentAOE.INSTANCE ? "area"
                    : augment == AugmentExtendTime.INSTANCE || augment == AugmentDurationDown.INSTANCE ? "duration" : "native_level";
            augmentDescriptions.put(augment, Component.translatable("irons_nouveau.augment." + key));
        }
    }
    @Override public void addAugmentDescriptions(Map<AbstractAugment, String> descriptions) {
        descriptions.put(AugmentAmplify.INSTANCE, "Adds one native spell level.");
        descriptions.put(AugmentDampen.INSTANCE, "Removes one native spell level, to a minimum of one.");
        descriptions.put(AugmentAOE.INSTANCE, "Affects nearby targets.");
    }
}
