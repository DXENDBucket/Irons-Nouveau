package dev.ironsnouveau.client;

import dev.ironsnouveau.casting.ChantHudTimer;
import dev.ironsnouveau.network.ChantStatePayload;
import io.redspace.ironsspellbooks.player.ClientMagicData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import java.lang.ref.WeakReference;

/** Used only by the native cast-bar renderer; does not alter native input, sounds, targeting or casting. */
public final class ChantHudState {
    private static final ChantHudTimer TIMER = new ChantHudTimer();
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private ChantHudState() {}
    public static void accept(ChantStatePayload data) {
        var client = Minecraft.getInstance();
        if (client.level != world.get()) TIMER.clear();
        world = new WeakReference<>(client.level);
        if (client.level != null) TIMER.update(data.token(), data.duration(), data.remaining(), client.level.getGameTime());
    }
    public static boolean visible() {
        var client = Minecraft.getInstance();
        var level = world.get();
        if (level == null || level != client.level || client.player == null || !client.player.isAlive()) {
            TIMER.clear(); world.clear();
            return false;
        }
        // Native Iron casts keep ownership of their own HUD when both systems are active.
        return !ClientMagicData.isCasting() && TIMER.active(level.getGameTime());
    }
    public static int duration() { return TIMER.duration(); }
    public static float progress() { return TIMER.progress(Minecraft.getInstance().level.getGameTime()); }
}
