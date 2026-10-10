package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/** Ars simulates trajectory/collision off-world; the only networked entity is the real Iron projectile. */
public final class ArsTrajectoryExecution implements CastExecution {
    private final AbstractMagicProjectile projectile;
    private final EntityProjectileSpell trajectory;
    private final ProjectilePayload payload;
    private final dev.arsconflux.api.projectile.ArsTrajectory motion;
    private int lastSplashExplosion = Integer.MIN_VALUE;
    public ArsTrajectoryExecution(AbstractMagicProjectile projectile, EntityProjectileSpell trajectory, ProjectilePayload payload) {
        this.projectile = projectile; this.trajectory = trajectory; this.payload = payload;
        motion = new dev.arsconflux.api.projectile.ArsTrajectory(projectile, trajectory);
    }
    public EntityProjectileSpell trajectory() { return trajectory; }
    @Override public net.minecraft.world.phys.Vec3 incomingDirection() { return TriggerGeometry.direction(trajectory); }
    @Override public boolean start(CastSession session) {
        projectile.setOwner(session.caster());
        return motion.start(session.world(), () -> {
            payload.configure(session, projectile);
            var carrier = (NativeCastCarrier)projectile;
            carrier.ironsNouveau$session(session); carrier.ironsNouveau$arsDriven(true);
        }, hit -> impact(session, hit));
    }
    private void impact(CastSession session, HitResult hit) {
        if (!session.active() || !session.permitted()) return;
        session.activate(() -> applyImpact(session, hit));
    }
    private boolean applyImpact(CastSession session, HitResult hit) {
        if (payload instanceof ComplexProjectilePayload complex) {
            if (CarrierProfiles.of(trajectory) == CarrierProfiles.SPLASH) {
                if (lastSplashExplosion == trajectory.tickCount) return false;
                lastSplashExplosion = trajectory.tickCount;
                if (hit instanceof EntityHitResult entityHit) hit = new EntityHitResult(entityHit.getEntity(), trajectory.position());
            }
            return complex.detonate(session, projectile, hit);
        }
        boolean success = hit.getType() == HitResult.Type.BLOCK;
        if (hit instanceof EntityHitResult target) {
            success = payload.apply(session, projectile, target.getEntity());
        }
        if (success) {
            var pos = hit.getLocation();
            projectile.impactParticles(pos.x, pos.y, pos.z);
            session.impact(hit);
        }
        return success;
    }
    @Override public boolean tick(CastSession session) { return motion.tick(session.world()); }
    @Override public void close(CastSession session, CastSession.EndReason reason) {
        ((NativeCastCarrier)projectile).ironsNouveau$session(null); motion.close();
    }
}
