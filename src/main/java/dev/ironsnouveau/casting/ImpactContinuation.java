package dev.ironsnouveau.casting;

import net.minecraft.world.phys.HitResult;

@FunctionalInterface
public interface ImpactContinuation {
    void resolve(HitResult hit);
    default void close() {}
}
