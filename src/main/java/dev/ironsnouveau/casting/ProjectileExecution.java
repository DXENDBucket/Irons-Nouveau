package dev.ironsnouveau.casting;

import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;

public final class ProjectileExecution implements CastExecution {
    private final AbstractMagicProjectile projectile;
    private final ProjectilePayload payload;
    public ProjectileExecution(AbstractMagicProjectile projectile) { this(projectile, ProjectilePayload.HALF_POWER); }
    public ProjectileExecution(AbstractMagicProjectile projectile, ProjectilePayload payload) {
        this.projectile = projectile; this.payload = payload;
    }
    public CastExecution withTrajectory(com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell trajectory) {
        return new ArsTrajectoryExecution(projectile, trajectory, payload);
    }
    @Override public boolean start(CastSession session) {
        var aim = session.aim();
        projectile.setOwner(session.caster());
        projectile.setPos(aim.origin());
        payload.configure(session, projectile);
        projectile.setDeltaMovement(aim.direction().scale(projectile.getSpeed() * session.plan().modifiers().speedMultiplier()));
        var velocity = projectile.getDeltaMovement();
        projectile.setYRot((float)(Math.atan2(velocity.x, velocity.z) * 180 / Math.PI));
        projectile.setXRot((float)(Math.atan2(velocity.y, velocity.horizontalDistance()) * 180 / Math.PI));
        projectile.yRotO = projectile.getYRot();
        projectile.xRotO = projectile.getXRot();
        ((NativeCastCarrier) projectile).ironsNouveau$session(session);
        return session.world().addFreshEntity(projectile);
    }
    @Override public boolean tick(CastSession session) { return projectile.isRemoved(); }
    @Override public net.minecraft.world.phys.Vec3 incomingDirection() { return projectile.getDeltaMovement().normalize(); }
    @Override public void close(CastSession session, CastSession.EndReason reason) {
        ((NativeCastCarrier) projectile).ironsNouveau$session(null);
        if (!projectile.isRemoved()) projectile.discard();
    }
}
