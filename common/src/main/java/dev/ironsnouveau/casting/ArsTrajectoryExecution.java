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
    private final java.util.Map<java.util.UUID,Integer> contacts=new java.util.HashMap<>();
    private boolean launchContactAvailable;
    public ArsTrajectoryExecution(AbstractMagicProjectile projectile, EntityProjectileSpell trajectory, ProjectilePayload payload) {
        this.projectile = projectile; this.trajectory = trajectory; this.payload = payload;
        motion = new dev.arsconflux.api.projectile.ArsTrajectory(projectile, trajectory);
        if(payload.retainsEntityContacts())motion.contactPolicy(contact->contact.hit() instanceof EntityHitResult);
    }
    public EntityProjectileSpell trajectory() { return trajectory; }
    @Override public net.minecraft.world.phys.Vec3 incomingDirection() { return TriggerGeometry.direction(trajectory); }
    @Override public boolean start(CastSession session) {
        var profile=dev.arsconflux.api.projectile.CarrierRegistry.of(trajectory);
        launchContactAvailable=profile!=null && !profile.repeated();
        projectile.setOwner(session.damageOwner());
        return motion.start(session.world(), () -> {
            payload.configure(session, projectile);
            var carrier = (NativeCastCarrier)projectile;
            carrier.ironsNouveau$session(session); carrier.ironsNouveau$arsDriven(true);
        }, hit -> impact(session, hit));
    }
    private void impact(CastSession session, HitResult hit) {
        if (!session.active() || !session.permitted()) return;
        var contact=dev.arsconflux.api.projectile.TrajectoryContacts.current(trajectory);
        java.util.function.BooleanSupplier action=()->applyImpact(session,hit);
        if(contact==null)activate(session,action);
        else contact.activate(work->activate(session,work),action);
    }
    private boolean activate(CastSession session,java.util.function.BooleanSupplier action){
        var profile=dev.arsconflux.api.projectile.CarrierRegistry.of(trajectory);
        if(!payload.retainsEntityContacts() || profile==null || profile.repeated())return session.activate(action);
        // Non-repeating forms paid at launch. That covers the first successful contact;
        // later contacts pay normally. One splash frame shares this payment across its targets.
        boolean success=session.activate(()->(launchContactAvailable || session.billingSource()==null)?action.getAsBoolean()
                :session.billingSource().trigger(session.plan().spellId(),session.plan().spellLevel(),action));
        if(success)launchContactAvailable=false;
        return success;
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
            int interval=payload.entityContactInterval();
            if(interval>0){
                var entity=target.getEntity();var previous=contacts.get(entity.getUUID());
                if(previous!=null && entity.tickCount-previous<interval)return false;
                contacts.put(entity.getUUID(),entity.tickCount);
            }
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
        contacts.clear();
    }
}
