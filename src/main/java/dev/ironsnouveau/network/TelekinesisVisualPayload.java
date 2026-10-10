package dev.ironsnouveau.network;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public record TelekinesisVisualPayload(TelekinesisVisualState data) implements CustomPacketPayload {
    public static final Type<TelekinesisVisualPayload> TYPE = new Type<>(dev.ironsnouveau.platform.Locations.id("irons_nouveau", "telekinesis_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TelekinesisVisualPayload> CODEC = StreamCodec.of(
            (b, p) -> p.data.write(b), b -> new TelekinesisVisualPayload(TelekinesisVisualState.read(b)));
    @Override public Type<TelekinesisVisualPayload> type() { return TYPE; }
    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(TYPE, CODEC, (payload, context) -> {
            if (FMLEnvironment.dist == Dist.CLIENT)
                context.enqueueWork(() -> dev.ironsnouveau.client.TelekinesisVisuals.accept(payload.data));
        });
    }
}
