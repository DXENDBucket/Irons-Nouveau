package dev.ironsnouveau.casting;

/** One runtime driver. Future targeted/channel drivers do not need to implement projectile methods. */
public interface CastExecution {
    default boolean occupiesCaster() { return false; }
    default java.util.List<dev.arsconflux.api.execution.ExecutionClaim> claims(CastSession session) {
        if(!occupiesCaster())return java.util.List.of();
        var actor=session.executor();
        return java.util.List.of(new dev.arsconflux.api.execution.ExecutionClaim(actor==null?session.caster():actor,dev.arsconflux.api.execution.ExecutionClaim.ACTIVITY,false));
    }
    boolean start(CastSession session);
    /** Return true on normal completion. The driver may sample live aim and emit successful impacts. */
    boolean tick(CastSession session);
    default net.minecraft.world.phys.Vec3 incomingDirection() { return net.minecraft.world.phys.Vec3.ZERO; }
    void close(CastSession session, CastSession.EndReason reason);
}
