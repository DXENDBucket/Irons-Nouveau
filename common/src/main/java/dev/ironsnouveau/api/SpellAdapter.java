package dev.ironsnouveau.api;

import dev.ironsnouveau.bridge.Resolution;
import net.minecraft.world.entity.LivingEntity;

/** Applies a spell to one selected target. Must not charge mana or enter Iron's cast lifecycle. */
@FunctionalInterface
public interface SpellAdapter {
    boolean apply(Resolution resolution, LivingEntity target);
}
