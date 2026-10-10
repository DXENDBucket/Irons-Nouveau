package dev.ironsnouveau.network;

import dev.ironsnouveau.casting.CastSession;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import java.util.UUID;

/** Visual lease only; never installs native casting/targeting data on either side. */
public record TelekinesisVisualState(UUID token, ResourceLocation dimension, int casterId, UUID casterUuid,
                                    int targetId, UUID targetUuid, int remaining) {
    public static TelekinesisVisualState of(CastSession session, LivingEntity target, int remaining) {
        return new TelekinesisVisualState(session.id(), session.world().dimension().location(), session.caster().getId(),
                session.caster().getUUID(), target.getId(), target.getUUID(), Math.min(10, Math.max(0, remaining)));
    }
    public void write(FriendlyByteBuf b) {
        b.writeUUID(token); b.writeResourceLocation(dimension); b.writeVarInt(casterId); b.writeUUID(casterUuid);
        b.writeVarInt(targetId); b.writeUUID(targetUuid); b.writeVarInt(remaining);
    }
    public static TelekinesisVisualState read(FriendlyByteBuf b) {
        return new TelekinesisVisualState(b.readUUID(), b.readResourceLocation(), b.readVarInt(), b.readUUID(),
                b.readVarInt(), b.readUUID(), b.readVarInt());
    }
}
