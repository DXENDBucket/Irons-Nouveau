package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.function.Consumer;

/** Ars simulates trajectory/collision off-world; the only networked entity is the real Iron projectile. */
public final class ArsTrajectoryExecution implements CastExecution {
    private final AbstractMagicProjectile projectile;
    private final EntityProjectileSpell trajectory;
    private final ProjectilePayload payload;
    private int lastSplashExplosion = Integer.MIN_VALUE;
    public ArsTrajectoryExecution(AbstractMagicProjectile projectile, EntityProjectileSpell trajectory, ProjectilePayload payload) {
        this.projectile = projectile; this.trajectory = trajectory; this.payload = payload;
    }
    public EntityProjectileSpell trajectory() { return trajectory; }
    @Override public net.minecraft.world.phys.Vec3 incomingDirection() { return TriggerGeometry.direction(trajectory); }
    @Override public boolean start(CastSession session) {
        if (trajectory instanceof TrailBudget trail) trail.ironsNouveau$budget(Integer.MAX_VALUE);
        projectile.setOwner(session.caster());
        projectile.setPos(trajectory.position());
        projectile.setDeltaMovement(trajectory.getDeltaMovement());
        payload.configure(session, projectile);
        var carrier = (NativeCastCarrier) projectile;
        carrier.ironsNouveau$session(session);
        carrier.ironsNouveau$arsDriven(true);
        var context = trajectory.resolver().spellContext.clone();
        trajectory.setResolver(new ImpactResolver(context, hit -> impact(session, hit)));
        return session.world().addFreshEntity(projectile);
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
    @Override public boolean tick(CastSession session) {
        if (projectile.isRemoved() || trajectory.isRemoved()) return true;
        if (!session.world().hasChunkAt(trajectory.blockPosition())
                || !session.world().hasChunkAt(BlockPos.containing(trajectory.getNextHitPosition()))) return true;
        ++trajectory.tickCount;
        trajectory.tick();
        projectile.setPos(trajectory.position());
        projectile.setDeltaMovement(trajectory.getDeltaMovement());
        // Orbit has no linear velocity: predict the next position for client-side visual movement.
        if (trajectory.getDeltaMovement().lengthSqr() < 1.0e-8)
            projectile.setDeltaMovement(trajectory.getNextHitPosition().subtract(trajectory.position()).scale(1.0 / 3.0));
        var velocity = projectile.getDeltaMovement();
        projectile.setYRot((float)(Math.atan2(velocity.x, velocity.z) * 180 / Math.PI));
        projectile.setXRot((float)(Math.atan2(velocity.y, velocity.horizontalDistance()) * 180 / Math.PI));
        projectile.hasImpulse = true;
        return trajectory.isRemoved();
    }
    @Override public void close(CastSession session, CastSession.EndReason reason) {
        ((NativeCastCarrier)projectile).ironsNouveau$session(null);
        projectile.discard(); trajectory.discard();
    }
    private static final class ImpactResolver extends SpellResolver {
        private final Consumer<HitResult> impact;
        private ImpactResolver(SpellContext context, Consumer<HitResult> impact) { super(context); this.impact = impact; }
        @Override public void onResolveEffect(Level level, HitResult hit) { if (!level.isClientSide) impact.accept(hit); }
        @Override public SpellResolver getNewResolver(SpellContext context) { return new ImpactResolver(context, impact); }
    }
}
