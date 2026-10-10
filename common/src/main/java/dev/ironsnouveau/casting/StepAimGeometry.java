package dev.ironsnouveau.casting;

import dev.ironsnouveau.bridge.Resolution;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.Vec3;

/** Native landing solver with a scoped direction; never changes the real caster's rotation. */
public final class StepAimGeometry {
    private record View(LivingEntity caster, Vec3 direction) {}
    private static final ThreadLocal<View> VIEW = new ThreadLocal<>();
    private StepAimGeometry() {}
    public static Vec3 forward(LivingEntity caster, Vec3 original) {
        var view = VIEW.get();
        return view != null && view.caster() == caster ? view.direction() : original;
    }
    public static Vec3 towards(Resolution ctx, Vec3 point, float range) {
        var start = ctx.caster().getEyePosition();
        var offset = point.subtract(start);
        if (!Double.isFinite(offset.lengthSqr()) || offset.lengthSqr() < .000001
                || !Float.isFinite(range) || range <= 0) return null;
        var direction = offset.normalize();
        var end = start.add(direction.scale(Math.min(range, offset.length())));
        if (!ctx.world().hasChunksAt(BlockPos.containing(start), BlockPos.containing(end))) return null;
        var hit = ctx.world().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx.caster()));
        var previous = VIEW.get();
        VIEW.set(new View(ctx.caster(), direction));
        try { return TeleportSpell.solveTeleportDestination(ctx.world(), ctx.caster(), hit.getBlockPos(), hit.getLocation()); }
        finally { if (previous == null) VIEW.remove(); else VIEW.set(previous); }
    }
}
