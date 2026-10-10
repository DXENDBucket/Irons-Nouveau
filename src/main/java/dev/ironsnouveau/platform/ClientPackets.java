package dev.ironsnouveau.platform;

import net.minecraft.server.level.ServerPlayer;
import dev.ironsnouveau.network.ChantStatePayload;
import dev.ironsnouveau.network.CastingMovementPayload;

public final class ClientPackets {
    private ClientPackets() {}
    public static void chant(ServerPlayer player, ChantStatePayload packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
    }
    public static void movement(ServerPlayer player, CastingMovementPayload packet) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, packet);
    }
}
