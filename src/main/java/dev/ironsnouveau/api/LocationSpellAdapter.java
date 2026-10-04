package dev.ironsnouveau.api;

import dev.ironsnouveau.bridge.Resolution;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;

/** A single location activation; avoids creating one explosion or summon group per area target. */
@FunctionalInterface
public interface LocationSpellAdapter extends SpellAdapter {
    boolean applyAt(Resolution context, HitResult hit);
    @Override default boolean apply(Resolution context, LivingEntity target) {
        return applyAt(context, new net.minecraft.world.phys.EntityHitResult(target));
    }
}
