package dev.ironsnouveau.casting;

import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Projectile policy; channel/target adapters can supply a different budgeted continuation. */
public final class OncePerTargetContinuation implements ImpactContinuation {
    private final ImpactContinuation delegate;
    private final Set<UUID> affected = new HashSet<>();
    private boolean hitBlock;
    public OncePerTargetContinuation(ImpactContinuation delegate) { this.delegate = delegate; }
    @Override public void resolve(HitResult hit) {
        if (hit instanceof EntityHitResult entityHit) {
            if (affected.size() >= 128 || !affected.add(entityHit.getEntity().getUUID())) return;
        } else if (hit.getType() == HitResult.Type.BLOCK) {
            if (hitBlock) return;
            hitBlock = true;
        } else return;
        delegate.resolve(hit);
    }
    @Override public void close() { affected.clear(); delegate.close(); }
}
