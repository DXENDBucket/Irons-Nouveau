package dev.ironsnouveau.network;

import dev.ironsnouveau.IronsNouveau;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Aggregate remaining lifetime of this player's movement leases. No native casting-state mutation. */
public record CastingMovementPayload(int entityId, ResourceLocation dimension, int remaining) implements CustomPacketPayload {
    public static final Type<CastingMovementPayload> TYPE = new Type<>(dev.ironsnouveau.platform.Locations.id(IronsNouveau.MOD_ID, "casting_movement"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastingMovementPayload> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeVarInt(data.entityId); buf.writeResourceLocation(data.dimension); buf.writeVarInt(data.remaining); },
            buf -> new CastingMovementPayload(buf.readVarInt(), buf.readResourceLocation(), buf.readVarInt()));
    @Override public Type<CastingMovementPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> {
            if (FMLEnvironment.dist == Dist.CLIENT)
                context.enqueueWork(() -> dev.ironsnouveau.client.CastingMovementState.accept(payload));
        });
    }
}
