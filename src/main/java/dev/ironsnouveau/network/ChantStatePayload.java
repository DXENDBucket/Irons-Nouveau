package dev.ironsnouveau.network;

import dev.ironsnouveau.IronsNouveau;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import java.util.UUID;

/** Visual state only. Never initiates an Iron cast or spends mana. remaining == 0 ends this token. */
public record ChantStatePayload(UUID token, int duration, int remaining) implements CustomPacketPayload {
    public static final Type<ChantStatePayload> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(IronsNouveau.MOD_ID, "chant_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ChantStatePayload> CODEC = StreamCodec.of(
            (buf, data) -> { buf.writeUUID(data.token); buf.writeVarInt(data.duration); buf.writeVarInt(data.remaining); },
            buf -> new ChantStatePayload(buf.readUUID(), buf.readVarInt(), buf.readVarInt()));
    @Override public Type<ChantStatePayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> {
            if (FMLEnvironment.dist == Dist.CLIENT)
                context.enqueueWork(() -> dev.ironsnouveau.client.ChantHudState.accept(payload));
        });
    }
}
