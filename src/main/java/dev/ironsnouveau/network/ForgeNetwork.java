package dev.ironsnouveau.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public final class ForgeNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation("irons_nouveau", "main"), () -> "2", "2"::equals, "2"::equals);
    private ForgeNetwork() {}
    public static void register() {
        CHANNEL.messageBuilder(ChantStatePayload.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p,b) -> { b.writeUUID(p.token()); b.writeVarInt(p.duration()); b.writeVarInt(p.remaining()); })
                .decoder(b -> new ChantStatePayload(b.readUUID(), b.readVarInt(), b.readVarInt()))
                .consumerMainThread((p,c) -> ClientReceiver.chant(p)).add();
        CHANNEL.messageBuilder(CastingMovementPayload.class, 1, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p,b) -> { b.writeVarInt(p.entityId()); b.writeResourceLocation(p.dimension()); b.writeVarInt(p.remaining()); })
                .decoder(b -> new CastingMovementPayload(b.readVarInt(), b.readResourceLocation(), b.readVarInt()))
                .consumerMainThread((p,c) -> ClientReceiver.movement(p)).add();
        CHANNEL.messageBuilder(ProgressPayload.class, 2, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((p,b) -> b.writeNbt(p.data().save()))
                .decoder(b -> new ProgressPayload(dev.ironsnouveau.progression.CraftedSpells.load(java.util.Objects.requireNonNull(b.readNbt()))))
                .consumerMainThread((p,c) -> ClientReceiver.progress(p)).add();
        CHANNEL.messageBuilder(SiphonRayVisualPayload.class, 3, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(SiphonRayVisualPayload::encode).decoder(SiphonRayVisualPayload::decode)
                .consumerMainThread((p,c) -> ClientReceiver.siphon(p)).add();
    }
    public static void sendNear(net.minecraft.server.level.ServerLevel world, net.minecraft.world.phys.Vec3 pos,
                                double radius, Object message) {
        CHANNEL.send(PacketDistributor.NEAR.with(() -> new PacketDistributor.TargetPoint(
                pos.x, pos.y, pos.z, radius, world.dimension())), message);
    }
    public static void sendToPlayer(ServerPlayer player, Object message) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
    // This class is referenced only by client-bound handlers, never loaded on a dedicated server.
    private static final class ClientReceiver {
        static void siphon(SiphonRayVisualPayload p) { dev.ironsnouveau.client.SiphonRayVisuals.accept(p); }
        static void chant(ChantStatePayload p) { dev.ironsnouveau.client.ChantHudState.accept(p); }
        static void movement(CastingMovementPayload p) { dev.ironsnouveau.client.CastingMovementState.accept(p); }
        static void progress(ProgressPayload p) {
            var player = net.minecraft.client.Minecraft.getInstance().player;
            if (player != null) dev.ironsnouveau.progression.SpellProgress.accept(player, p.data());
        }
    }
}
