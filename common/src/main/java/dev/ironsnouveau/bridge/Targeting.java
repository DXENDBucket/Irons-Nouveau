package dev.ironsnouveau.bridge;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class Targeting {
    private Targeting() {}
    /** A hostile recipient may be attacked, but cannot be forced to execute a beneficial action. */
    public static boolean alliedExecutor(LivingEntity caster, LivingEntity host) {
        return caster == host || caster.isAlliedTo(host) || host.isAlliedTo(caster);
    }
    public static boolean beneficialHost(LivingEntity caster, HitResult hit) {
        return !(hit instanceof EntityHitResult selected) || selected.getEntity() instanceof LivingEntity host
                && host.isAlive() && !host.isSpectator() && alliedExecutor(caster, host);
    }
    public static List<LivingEntity> select(ServerLevel world, LivingEntity caster, HitResult hit,
                                            double radius, boolean harmful) {
        var targets = new ArrayList<LivingEntity>();
        if (hit.getType() == HitResult.Type.MISS) return targets;
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity living
                && eligible(caster, living, harmful)) targets.add(living);
        if (radius > 0) {
            var center = hit.getLocation();
            var nearby = world.getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                    entity -> eligible(caster, entity, harmful) && !targets.contains(entity)
                            && entity.distanceToSqr(center) <= radius * radius
                            && caster.hasLineOfSight(entity));
            nearby.sort(Comparator.comparingDouble(entity -> entity.distanceToSqr(center)));
            targets.addAll(nearby.subList(0, Math.min(64 - targets.size(), nearby.size())));
        }
        return List.copyOf(targets);
    }
    private static boolean eligible(LivingEntity caster, LivingEntity target, boolean harmful) {
        return target.isAlive() && !target.isSpectator()
                && (!harmful || (target != caster && !caster.isAlliedTo(target)));
    }
}
