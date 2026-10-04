package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import net.minecraft.world.phys.Vec3;

/** Immutable direction snapshot travels with cloned/delayed Ars contexts, independently of their owner. */
public final class TriggerGeometry {
    private static final String KEY = "irons_nouveau_incoming";
    private static final ThreadLocal<Vec3> CURRENT = new ThreadLocal<>();
    private TriggerGeometry() {}
    public static void write(SpellContext context, Vec3 direction) {
        if (context == null || direction == null || direction.lengthSqr() < 1.0e-10) return;
        var tag = new net.minecraft.nbt.CompoundTag(); direction = direction.normalize();
        tag.putDouble("x", direction.x); tag.putDouble("y", direction.y); tag.putDouble("z", direction.z);
        context.tag.put(KEY, tag);
    }
    public static Vec3 read(SpellContext context) {
        if (CURRENT.get() != null) return CURRENT.get();
        if (context == null || !context.tag.contains(KEY)) return Vec3.ZERO;
        var tag = context.tag.getCompound(KEY);
        return new Vec3(tag.getDouble("x"), tag.getDouble("y"), tag.getDouble("z")).normalize();
    }
    public static void capture(SpellContext context) { if (CURRENT.get() != null) write(context, CURRENT.get()); }
    public static Vec3 direction(EntityProjectileSpell projectile) {
        var direction = projectile.getDeltaMovement();
        return direction.lengthSqr() > 1.0e-10 ? direction.normalize()
                : projectile.getNextHitPosition().subtract(projectile.position()).normalize();
    }
    public static void scoped(Vec3 direction, Runnable action) {
        var previous = CURRENT.get();
        if (direction != null && direction.lengthSqr() > 1.0e-10) CURRENT.set(direction.normalize());
        try { action.run(); } finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
}
