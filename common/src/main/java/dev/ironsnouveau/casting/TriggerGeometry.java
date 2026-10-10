package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import net.minecraft.world.phys.Vec3;

/** Kept for callers; direction snapshots and their Ars hooks are owned by Conflux. */
public final class TriggerGeometry {
    private TriggerGeometry() {}
    public static void write(SpellContext context, Vec3 direction) { dev.arsconflux.api.context.TriggerGeometry.write(context, direction); }
    public static Vec3 read(SpellContext context) { return dev.arsconflux.api.context.TriggerGeometry.read(context); }
    public static void capture(SpellContext context) { dev.arsconflux.api.context.TriggerGeometry.capture(context); }
    public static Vec3 direction(EntityProjectileSpell projectile) { return dev.arsconflux.api.context.TriggerGeometry.direction(projectile); }
    public static void scoped(Vec3 direction, Runnable action) { dev.arsconflux.api.context.TriggerGeometry.scoped(direction, action); }
}
