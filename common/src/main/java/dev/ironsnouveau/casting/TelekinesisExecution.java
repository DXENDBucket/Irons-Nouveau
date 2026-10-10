package dev.ironsnouveau.casting;

import dev.ironsnouveau.bridge.EntitySelectionAdapters;
import dev.ironsnouveau.bridge.Resolution;
import dev.ironsnouveau.mixin.TelekinesisAccess;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.TelekinesisData;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.EntityHitResult;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Fixed-duration channel, native physics, isolated cast data. One controller per selected entity. */
public final class TelekinesisExecution implements CastExecution {
    private static final Map<UUID, CastSession> CONTROLLERS = new HashMap<>();
    private final Resolution resolution;
    private final LivingEntity target;
    private final MagicData data = new MagicData();
    private int elapsed;
    private TelekinesisExecution(Resolution resolution, LivingEntity target, int ticks) {
        this.resolution = resolution; this.target = target;
        // Detached data has no entity to synchronize; initialize its lazy state before native initiateCast.
        data.getSyncedData();
        data.initiateCast(resolution.spell(), resolution.level(), ticks, CastSource.NONE, "");
        data.setAdditionalCastData(new TelekinesisData(resolution.caster().distanceTo(target), target, 6));
    }
    public static boolean start(Resolution ctx, LivingEntity target) {
        int ticks = EffectResources.ticks(ctx.spell().getCastTime(ctx.level()) * ctx.duration());
        var frame = ctx.context().withExecutor(ctx.caster()).withTarget(new EntityHitResult(target));
        var plan = new CastPlan(ctx.definition().glyphId(), ctx.definition().spellId(), ctx.level(), (float)ctx.power(),
                new CastModifiers(0, 1), ticks);
        return CastSessions.start(new CastSession(ctx.world(), frame, plan,
                () -> new CastAim(ctx.caster().getEyePosition(), ctx.caster().getLookAngle(), target.getUUID()),
                new TelekinesisExecution(ctx, target, ticks), ignored -> {}).billing(TriggerMana.of(frame), true));
    }
    private boolean valid() { return EntitySelectionAdapters.target(resolution, new EntityHitResult(target)) != null; }
    @Override public boolean occupiesCaster() { return true; }
    @Override public boolean start(CastSession session) {
        if (!valid()) return false;
        var previous = CONTROLLERS.put(target.getUUID(), session);
        if (previous != null) previous.finish(CastSession.EndReason.COMPLETED);
        force();
        MovementRestrictions.begin(session.caster(), session.id(), session.remainingTicks());
        syncVisual(session, session.remainingTicks());
        resolution.spell().getCastStartSound().ifPresent(sound -> session.world().playSound(null,
                session.caster().getX(), session.caster().getY(), session.caster().getZ(), sound,
                net.minecraft.sounds.SoundSource.PLAYERS, .7f, 1));
        return true;
    }
    private void syncVisual(CastSession session, int remaining) {
        var state = dev.ironsnouveau.network.TelekinesisVisualState.of(session, target, remaining);
        dev.ironsnouveau.platform.ClientPackets.telekinesis(session.world(), target.position(), state);
        // The caster may be outside the target's tracking range and still needs animation/cleanup.
        if (session.caster().distanceToSqr(target) > 48 * 48)
            dev.ironsnouveau.platform.ClientPackets.telekinesis(session.world(), session.caster().position(), state);
    }
    private void force() {
        ((TelekinesisAccess)resolution.spell()).ironsNouveau$handle(resolution.world(), resolution.caster(), data, .6f);
        if (target instanceof ServerPlayer player) player.connection.send(new ClientboundSetEntityMotionPacket(target));
    }
    @Override public boolean tick(CastSession session) {
        if (!valid()) return true;
        elapsed++;
        data.handleCastDuration();
        // First activation was paid by the glyph. Further half-second pulses use the same original account.
        if (elapsed % 10 == 0) {
            if (!session.activate(() -> { force(); return true; })) return true;
            resolution.spell().getCastFinishSound().ifPresent(sound -> session.world().playSound(null,
                    session.caster().getX(), session.caster().getY(), session.caster().getZ(), sound,
                    net.minecraft.sounds.SoundSource.PLAYERS, .7f, 1));
        } else if (elapsed % 2 == 0) force();
        if (elapsed % 5 == 0) syncVisual(session, session.remainingTicks());
        return false;
    }
    @Override public void close(CastSession session, CastSession.EndReason reason) {
        CONTROLLERS.remove(target.getUUID(), session);
        try { syncVisual(session, 0); }
        finally {
            MovementRestrictions.end(session.caster(), session.id());
            data.resetAdditionalCastData();
        }
    }
}
