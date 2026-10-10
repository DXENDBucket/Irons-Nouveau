package dev.ironsnouveau.casting;

import dev.ironsnouveau.bridge.Resolution;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.spells.ice.RayOfFrostSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Substitute only spatial input while executing Iron's complete instantaneous ray spell. */
public final class NativeRayGeometry {
    public record View(Vec3 origin, HitResult hit) {}
    private static final ThreadLocal<View> VIEW = new ThreadLocal<>();
    private NativeRayGeometry() {}
    public static View current() { return VIEW.get(); }

    public static boolean frost(Resolution ctx, HitResult trigger) {
        var pose = rayPose(ctx, trigger);
        var aim = pose.sample();
        Vec3 start = aim.origin(), end = start.add(aim.direction().normalize()
                .scale(RayOfFrostSpell.getRange(ctx.level(), ctx.caster())));
        if (!ctx.world().hasChunksAt(BlockPos.containing(start), BlockPos.containing(end))) return false;
        HitResult hit = ctx.world().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, ctx.caster()));
        Vec3 clippedEnd = hit.getLocation();
        // Native RaycastBuilder searches from the owner's bounding box. Remote triggers must
        // instead search the actual ray, retaining Iron's filtering and intersection routine.
        double nearest = start.distanceToSqr(clippedEnd);
        for (var entity : ctx.world().getEntities(ctx.caster(), new AABB(start, clippedEnd).inflate(.15),
                e -> e != pose.anchor() && Utils.canHitWithRaycast(e))) {
            var candidate = Utils.checkEntityIntersecting(entity, start, clippedEnd, .15f);
            if (candidate.getType() == HitResult.Type.MISS) continue;
            double distance = start.distanceToSqr(candidate.getLocation());
            if (distance < nearest) { nearest = distance; hit = candidate; }
        }
        var previous = VIEW.get(); VIEW.set(new View(start, hit));
        try {
            var selected = ctx.withTarget(trigger).withExecutor(pose.anchor());
            return dev.arsconflux.api.context.CastContexts.scoped(selected.context(), () -> EffectResources.paidChildren(selected, 20, () -> {
                // No mutation of the caster's native casting state. Iron handles damage,
                // freezing, both visual layers, hit fog and endpoint snowflakes itself.
                ctx.spell().onCast(ctx.world(), ctx.level(), ctx.caster(), CastSource.NONE, new MagicData());
                return true;
            }));
        } finally { if (previous == null) VIEW.remove(); else VIEW.set(previous); }
    }

    public static BreathPose rayPose(Resolution ctx, HitResult hit) {
        if (hit instanceof EntityHitResult entity && entity.getEntity() instanceof net.minecraft.world.entity.LivingEntity living)
            return new BreathPose(living, living.getEyePosition(), living.getLookAngle(), Vec3.ZERO, true);
        return BreathPose.from(ctx, hit);
    }
}
