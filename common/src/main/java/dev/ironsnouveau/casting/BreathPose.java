package dev.ironsnouveau.casting;
import dev.ironsnouveau.bridge.Resolution;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;

/** Entity hit wins over incoming projectile direction. Entity offsets follow movement; point hits stay fixed. */
public record BreathPose(LivingEntity anchor, Vec3 origin, Vec3 direction, Vec3 offset, boolean eyeAnchored) implements AimSource {
    public BreathPose(LivingEntity anchor, Vec3 origin, Vec3 direction, Vec3 offset) {
        this(anchor, origin, direction, offset, false);
    }
    /** Iron positions the collision cone below the eyes; particle origins are handled separately. */
    public static BreathPose forBreath(Resolution ctx, HitResult hit) {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living) {
            var offset = new Vec3(0, -.8, 0);
            return new BreathPose(living, living.getEyePosition().add(offset), living.getLookAngle(), offset, true);
        }
        return from(ctx, hit);
    }
    public static BreathPose from(Resolution ctx, HitResult hit) {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living)
            return new BreathPose(living, hit.getLocation(), living.getLookAngle(), hit.getLocation().subtract(living.position()));
        Vec3 direction = ctx.incomingDirection().lengthSqr() > 1.0e-10 ? ctx.incomingDirection() : ctx.caster().getLookAngle();
        return new BreathPose(null, hit.getLocation(), direction.normalize(), Vec3.ZERO);
    }
    @Override public CastAim sample() {
        return anchor == null ? new CastAim(origin, direction, null)
                : new CastAim((eyeAnchored ? anchor.getEyePosition() : anchor.position()).add(offset), anchor.getLookAngle(), anchor.getUUID());
    }
    public boolean valid(CastSession session) { return anchor == null || anchor.isAlive() && !anchor.isRemoved() && anchor.level() == session.world(); }
}
