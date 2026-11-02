package dev.ironsnouveau.ability;
/** A single synced origin lets late trackers render the native fuse/charge animation at its real age. */
public interface OrbClock {
    long ironsNouveau$startedAt();
    void ironsNouveau$startedAt(long tick);
}
