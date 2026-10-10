package dev.ironsnouveau.platform;

import net.minecraft.server.level.ServerPlayer;
import dev.ironsnouveau.network.ChantStatePayload;
import dev.ironsnouveau.network.CastingMovementPayload;

public final class ClientPackets {
    private ClientPackets() {}
    public static void telekinesis(net.minecraft.server.level.ServerLevel world, net.minecraft.world.phys.Vec3 pos,
                                   dev.ironsnouveau.network.TelekinesisVisualState data) {
        dev.ironsnouveau.network.ForgeNetwork.sendNear(world, pos, 96, new dev.ironsnouveau.network.TelekinesisVisualPayload(data));
    }
    public static void chant(ServerPlayer player, ChantStatePayload packet) {
        dev.ironsnouveau.network.ForgeNetwork.sendToPlayer(player, packet);
    }
    public static void movement(ServerPlayer player, CastingMovementPayload packet) {
        dev.ironsnouveau.network.ForgeNetwork.sendToPlayer(player, packet);
    }
}
