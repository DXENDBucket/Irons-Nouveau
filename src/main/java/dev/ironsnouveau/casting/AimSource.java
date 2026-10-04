package dev.ironsnouveau.casting;

/** Sampled by the execution. Fixed shots and live aiming use the same boundary. */
@FunctionalInterface
public interface AimSource {
    CastAim sample();
    static AimSource fixed(CastAim aim) { return () -> aim; }
}
