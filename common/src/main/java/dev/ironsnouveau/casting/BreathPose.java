package dev.ironsnouveau.casting;
import dev.ironsnouveau.bridge.Resolution;
import dev.arsconflux.api.execution.ExecutionAnchor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;

/** Entity hit wins over incoming projectile direction. Entity offsets follow movement; point hits stay fixed. */
public record BreathPose(LivingEntity anchor, Vec3 origin, Vec3 direction, Vec3 offset, boolean eyeAnchored) implements AimSource {
    public BreathPose(LivingEntity anchor, Vec3 origin, Vec3 direction, Vec3 offset) {
        this(anchor, origin, direction, offset, false);
    }
    /** Iron positions the collision cone below the eyes; particle origins are handled separately. */
    public static BreathPose forBreath(Resolution ctx, HitResult hit) {
        var source=ExecutionAnchor.beneficial(ctx.context().withTarget(hit),ExecutionAnchor.Basis.EYES,new Vec3(0,-.8,0));
        if(source==null)return null;
        var pose=source.sample();
        return new BreathPose(source.entity(),pose.origin(),pose.direction(),source.offset(),source.entity()!=null);
    }
    public static BreathPose from(Resolution ctx, HitResult hit) {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living)
            return new BreathPose(living, hit.getLocation(), living.getLookAngle(), hit.getLocation().subtract(living.position()));
        Vec3 direction = ctx.incomingDirection().lengthSqr() > 1.0e-10 ? ctx.incomingDirection() : ctx.caster().getLookAngle();
        return new BreathPose(null, hit.getLocation(), direction.normalize(), Vec3.ZERO);
    }
    @Override public CastAim sample() {
        var pose=executionAnchor().sample();
        return new CastAim(pose.origin(),pose.direction(),anchor==null?null:anchor.getUUID());
    }
    public ExecutionAnchor executionAnchor(){return anchor==null?ExecutionAnchor.position(origin,direction)
            :ExecutionAnchor.entity(anchor,eyeAnchored?ExecutionAnchor.Basis.EYES:ExecutionAnchor.Basis.FEET,offset);}
    public boolean valid(CastSession session) {return executionAnchor().valid(session.world(),session.context());}
}
