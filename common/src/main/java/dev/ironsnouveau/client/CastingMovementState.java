package dev.ironsnouveau.client;

import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.network.CastingMovementPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import java.lang.ref.WeakReference;

public final class CastingMovementState {
    private static WeakReference<ClientLevel> world = new WeakReference<>(null);
    private static long expires;
    private static int entityId;
    private CastingMovementState() {}
    public static void accept(CastingMovementPayload data) {
        var client = Minecraft.getInstance();
        if (client.level == null || client.player == null || client.player.getId() != data.entityId()
                || !client.level.dimension().location().equals(data.dimension())) return;
        world = new WeakReference<>(client.level);
        entityId = data.entityId();
        expires = client.level.getGameTime() + Math.max(0, data.remaining());
    }
    public static boolean active() {
        var client = Minecraft.getInstance();
        var level = world.get();
        if (level == null || level != client.level || client.player == null || client.player.getId() != entityId
                || !client.player.isAlive()) {
            world.clear(); expires = 0;
            return false;
        }
        return SpellLevelConfig.movementEnabled() && !client.player.isSpectator() && level.getGameTime() < expires;
    }
}
