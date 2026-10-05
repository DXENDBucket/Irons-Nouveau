package dev.ironsnouveau.casting;

import dev.ironsnouveau.bridge.Resolution;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.HitResult;
import java.util.function.BiPredicate;

/** Repeated activations share the original account, level, pose and lifecycle. */
public final class TimedEffects implements CastExecution {
    private final BreathPose pose;
    private final int interval;
    private final BiPredicate<CastSession, CastAim> action;
    private long next;
    private TimedEffects(BreathPose pose, int interval, BiPredicate<CastSession, CastAim> action) {
        this.pose = pose; this.interval = interval; this.action = action;
    }
    public static boolean start(Resolution ctx, HitResult hit, int interval, int duration,
                                boolean followTarget, BiPredicate<CastSession, CastAim> action) {
        var pose = BreathPose.from(ctx, hit);
        if (!followTarget) pose = new BreathPose(null, hit.getLocation(), pose.sample().direction(), net.minecraft.world.phys.Vec3.ZERO);
        var plan = new CastPlan(ctx.definition().glyphId(), ctx.definition().spellId(), ctx.level(), (float)ctx.power(),
                new CastModifiers(0, 1), EffectResources.ticks(duration));
        var account = TriggerMana.current() == null ? TriggerMana.of(null, ctx.caster()) : TriggerMana.current();
        return CastSessions.start(new CastSession(ctx.world(), ctx.caster(), plan, pose,
                new TimedEffects(pose, interval, action), ignored -> {}).billing(account, true));
    }
    @Override public boolean start(CastSession session) {
        if (!valid(session)) return false;
        next = session.world().getGameTime() + interval;
        // The outer glyph pays for the first activation.
        return action.test(session, pose.sample());
    }
    private boolean valid(CastSession session) {
        return pose.valid(session) && session.world().hasChunkAt(BlockPos.containing(pose.sample().origin()));
    }
    @Override public boolean tick(CastSession session) {
        if (!valid(session)) return true;
        if (session.world().getGameTime() >= next) {
            next = session.world().getGameTime() + interval;
            if (!session.activate(() -> action.test(session, pose.sample()))) return true;
        }
        return false;
    }
    @Override public void close(CastSession session, CastSession.EndReason reason) {}
}
