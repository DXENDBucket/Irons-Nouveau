package dev.ironsnouveau.casting;

import java.util.UUID;

/** Client countdown, periodically corrected by the server. Independent of Iron MagicData. */
public final class ChantHudTimer {
    private UUID token;
    private int duration;
    private int remaining;
    private long syncedAt;
    public void update(UUID token, int duration, int remaining, long now) {
        if (remaining <= 0) {
            // A cancelled old cast must never erase a newer cast's display.
            if (token.equals(this.token)) clear();
            return;
        }
        if (duration <= 0) return;
        this.token = token;
        this.duration = duration;
        this.remaining = Math.min(remaining, duration);
        this.syncedAt = now;
    }
    public void clear() { token = null; duration = 0; remaining = 0; }
    public boolean active(long now) { return token != null && now >= syncedAt && remaining(now) > 0; }
    public int duration() { return duration; }
    public int remaining(long now) { return (int)Math.max(0, remaining - Math.max(0L, now - syncedAt)); }
    public float progress(long now) { return duration == 0 ? 0 : 1f - remaining(now) / (float)duration; }
}
