package dev.ironsnouveau.casting;

/** Pending modifiers. Native level/power and velocity are resolved only when execution begins. */
public record CastModifiers(int levelDelta, double speedMultiplier) {
    public int resolveLevel(int base) {
        return (int) net.minecraft.util.Mth.clamp((long) base + levelDelta, 1, Integer.MAX_VALUE);
    }
}
