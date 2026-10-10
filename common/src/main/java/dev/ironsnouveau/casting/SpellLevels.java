package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import java.util.List;

/** Shared native level policy for forms, effects, permission checks and their lasting executions. */
public final class SpellLevels {
    public static final int DEFAULT = 1;
    private SpellLevels() {}
    public static int delta(List<AbstractAugment> augments) {
        int delta = 0;
        for (var augment : augments) {
            if (augment == AugmentAmplify.INSTANCE) delta++;
            else if (augment == AugmentDampen.INSTANCE) delta--;
        }
        return delta;
    }
    public static int resolve(List<AbstractAugment> augments) {
        return new CastModifiers(delta(augments), 1).resolveLevel(DEFAULT);
    }
    public static int resolve(net.minecraft.world.entity.LivingEntity caster, net.minecraft.resources.ResourceLocation spell,
                              List<AbstractAugment> augments) {
        return new CastModifiers(delta(augments), 1).resolveLevel(dev.ironsnouveau.progression.SpellProgress.baseLevel(caster, spell));
    }
}
