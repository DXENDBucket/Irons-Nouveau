package dev.ironsnouveau.casting;

/** Applied to native entities by a conditional mixin; ordinary Iron projectiles have no session. */
public interface NativeCastCarrier extends dev.arsconflux.api.projectile.CarrierProxy {
    @Override default com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell confluxTrajectory() {
        if(this instanceof dev.ironsnouveau.ability.DaggerState dagger&&dagger.ironsNouveau$trajectory()!=null)return dagger.ironsNouveau$trajectory();
        var session = ironsNouveau$session();
        return session != null && session.execution() instanceof ArsTrajectoryExecution execution ? execution.trajectory() : null;
    }
    CastSession ironsNouveau$session();
    void ironsNouveau$session(CastSession session);
    boolean ironsNouveau$arsDriven();
    void ironsNouveau$arsDriven(boolean value);
}
