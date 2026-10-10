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
    @Override public boolean start(CastSession session) {
        if (!valid()) return false;
        var previous = CONTROLLERS.put(target.getUUID(), session);
        if (previous != null) previous.finish(CastSession.EndReason.COMPLETED);
        force();
        return true;
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
        } else if (elapsed % 2 == 0) force();
        return false;
    }
    @Override public void close(CastSession session, CastSession.EndReason reason) {
        CONTROLLERS.remove(target.getUUID(), session);
        data.resetAdditionalCastData();
    }
}
