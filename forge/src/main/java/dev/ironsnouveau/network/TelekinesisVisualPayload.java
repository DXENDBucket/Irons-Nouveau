package dev.ironsnouveau.network;
import net.minecraft.network.FriendlyByteBuf;
public record TelekinesisVisualPayload(TelekinesisVisualState data) {
    public static void encode(TelekinesisVisualPayload p, FriendlyByteBuf b) { p.data.write(b); }
    public static TelekinesisVisualPayload decode(FriendlyByteBuf b) { return new TelekinesisVisualPayload(TelekinesisVisualState.read(b)); }
}
