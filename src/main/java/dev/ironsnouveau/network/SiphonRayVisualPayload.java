package dev.ironsnouveau.network;

import dev.ironsnouveau.IronsNouveau;
import dev.ironsnouveau.casting.BreathPose;
import dev.ironsnouveau.casting.CastSession;
import io.redspace.ironsspellbooks.spells.blood.RayOfSiphoningSpell;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Visual-only lease. No native casting state, mana, damage or cooldown is changed on the client. */
public record SiphonRayVisualPayload(UUID token, ResourceLocation dimension, int anchorId, UUID anchorUuid,
                                    UUID ownerUuid, Vec3 origin, Vec3 direction, Vec3 offset,
                                    boolean eyeAnchored, float range, int remaining) {
    public static void encode(SiphonRayVisualPayload p, FriendlyByteBuf b) {
        b.writeUUID(p.token); b.writeResourceLocation(p.dimension); b.writeVarInt(p.anchorId);
        b.writeUUID(p.anchorUuid); b.writeUUID(p.ownerUuid);
        write(b, p.origin); write(b, p.direction); write(b, p.offset);
        b.writeBoolean(p.eyeAnchored); b.writeFloat(p.range); b.writeVarInt(p.remaining);
    }
    public static SiphonRayVisualPayload decode(FriendlyByteBuf b) {
        return new SiphonRayVisualPayload(b.readUUID(), b.readResourceLocation(), b.readVarInt(),
                b.readUUID(), b.readUUID(), read(b), read(b), read(b), b.readBoolean(), b.readFloat(), b.readVarInt());
    }
    private static void write(FriendlyByteBuf b, Vec3 v) { b.writeDouble(v.x); b.writeDouble(v.y); b.writeDouble(v.z); }
    private static Vec3 read(FriendlyByteBuf b) { return new Vec3(b.readDouble(), b.readDouble(), b.readDouble()); }
    public static SiphonRayVisualPayload of(CastSession session, BreathPose pose, int remaining) {
        var aim = pose.sample(); var anchor = pose.anchor();
        return new SiphonRayVisualPayload(session.id(), session.world().dimension().location(),
                anchor == null ? -1 : anchor.getId(), anchor == null ? new UUID(0, 0) : anchor.getUUID(),
                session.caster().getUUID(), aim.origin(), aim.direction(), pose.offset(), pose.eyeAnchored(),
                RayOfSiphoningSpell.getRange(session.plan().spellLevel()), Math.min(10, Math.max(0, remaining)));
    }
}
