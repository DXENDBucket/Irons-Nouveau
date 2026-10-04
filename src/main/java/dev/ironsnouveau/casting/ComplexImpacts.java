package dev.ironsnouveau.casting;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.*;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
public final class ComplexImpacts {
    private static final ThreadLocal<Impact> CURRENT = new ThreadLocal<>();
    private record Impact(AbstractMagicProjectile projectile, CastSession session, Set<UUID> victims) {}
    private ComplexImpacts() {}
    public static void execute(AbstractMagicProjectile projectile, HitResult hit, Runnable action) {
        var session = ((NativeCastCarrier)projectile).ironsNouveau$session();
        if (session == null) { action.run(); return; }
        if (!session.permitted()) return;
        var previous = CURRENT.get();
        if (previous != null && previous.projectile() == projectile) { action.run(); return; }
        var impact = new Impact(projectile, session, new HashSet<>()); CURRENT.set(impact);
        try {
            EffectResources.scoped(session, () -> { action.run(); return true; });
            if (hit.getType() == HitResult.Type.BLOCK) session.impact(hit);
        } finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    public static void damaged(Entity target, DamageSource source) {
        var impact = CURRENT.get();
        if (impact != null && source.getDirectEntity() == impact.projectile() && impact.victims().add(target.getUUID()))
            impact.session().impact(new EntityHitResult(target));
    }
    public static void statusApplied(Entity target) {
        var impact = CURRENT.get();
        if (impact != null && impact.victims().add(target.getUUID())) impact.session().impact(new EntityHitResult(target));
    }
}
