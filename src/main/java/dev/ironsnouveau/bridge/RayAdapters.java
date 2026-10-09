package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.casting.*;

/** Rays share the breath anchor/direction/lifetime contract, but originate exactly at the eyes. */
public final class RayAdapters {
    private RayAdapters() {}
    public static final LocationSpellAdapter FROST = NativeRayGeometry::frost;
    public static final LocationSpellAdapter SIPHON = (ctx, hit) -> {
        var pose = NativeRayGeometry.rayPose(ctx, hit);
        int duration = EffectResources.ticks(ctx.spell().getCastTime(ctx.level()) * ctx.duration());
        var plan = new CastPlan(ctx.definition().glyphId(), ctx.definition().spellId(), ctx.level(),
                ctx.spell().getSpellPower(ctx.level(), ctx.caster()), new CastModifiers(0, 1), duration);
        var account = TriggerMana.current() == null ? TriggerMana.of(null, ctx.caster()) : TriggerMana.current();
        return CastSessions.start(new CastSession(ctx.world(), ctx.caster(), plan, pose,
                new SiphonRayExecution(pose), ignored -> {}).billing(account, true));
    };
}
