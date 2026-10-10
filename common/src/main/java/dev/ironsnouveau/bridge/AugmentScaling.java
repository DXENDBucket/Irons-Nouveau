package dev.ironsnouveau.bridge;

/** Spatial and duration modifiers applied after native level-dependent parameters. */
public final class AugmentScaling {
    private AugmentScaling() {}
    public static double duration(double extend) { return clamp(1 + .5 * extend, .1, 8); }
    public static double radius(double aoe) { return clamp(2 * aoe, 0, 8); }
    private static double clamp(double value, double min, double max) {
        return Double.isFinite(value) ? net.minecraft.util.Mth.clamp(value, min, max) : min;
    }
}
