package dev.ironsnouveau.platform;

import net.minecraft.server.level.ServerPlayer;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.network.SyncManaPacket;

public final class IronManaSync {
    private IronManaSync() {}
    public static void send(ServerPlayer player, MagicData data) {
        io.redspace.ironsspellbooks.setup.Messages.sendToPlayer(new SyncManaPacket(data), player);
    }
}
