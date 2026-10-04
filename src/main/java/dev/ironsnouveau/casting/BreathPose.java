package dev.ironsnouveau.casting;
import dev.ironsnouveau.bridge.Resolution;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;

/** Entity hit wins over incoming projectile direction. Entity offsets follow movement; point hits stay fixed. */
public record BreathPose(LivingEntity anchor, Vec3 origin, Vec3 direction, Vec3 offset) implements AimSource {
    public static BreathPose from(Resolution ctx, HitResult hit) {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living)
            return new BreathPose(living, hit.getLocation(), living.getLookAngle(), hit.getLocation().subtract(living.position()));
        Vec3 direction = ctx.incomingDirection().lengthSqr() > 1.0e-10 ? ctx.incomingDirection() : ctx.caster().getLookAngle();
        return new BreathPose(null, hit.getLocation(), direction.normalize(), Vec3.ZERO);
    }
    @Override public CastAim sample() {
        return anchor == null ? new CastAim(origin, direction, null)
                : new CastAim(anchor.position().add(offset), anchor.getLookAngle(), anchor.getUUID());
    }
    public boolean valid(CastSession session) { return anchor == null || anchor.isAlive() && !anchor.isRemoved() && anchor.level() == session.world(); }
}
