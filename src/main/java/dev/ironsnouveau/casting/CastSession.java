package dev.ironsnouveau.casting;

import dev.ironsnouveau.api.GlyphAccessEvent;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import java.util.UUID;

/** Server-owned lifecycle shared by execution kinds; continuation policy is supplied per session. */
public final class CastSession {
    public enum State { PREPARED, ACTIVE, COMPLETED, CANCELLED }
    public enum EndReason { COMPLETED, EXPIRED, OWNER_GONE, DENIED, WORLD_UNLOADED, SPAWN_FAILED, SERVER_STOPPED }
    private final UUID id = UUID.randomUUID();
    private final ServerLevel world;
    private final LivingEntity caster;
    private final CastPlan plan;
    private final AimSource aim;
    private final CastExecution execution;
    private final ImpactContinuation continuation;
    private TriggerMana mana;
    private boolean repeatPayments;
    private State state = State.PREPARED;
    private final long started;

    public CastSession(ServerLevel world, LivingEntity caster, CastPlan plan, AimSource aim,
                       CastExecution execution, ImpactContinuation continuation) {
        this.world = world; this.caster = caster; this.plan = plan; this.aim = aim;
        this.execution = execution; this.continuation = continuation;
        started = world.getGameTime();
    }
    public UUID id() { return id; }
    public ServerLevel world() { return world; }
    public LivingEntity caster() { return caster; }
    public CastPlan plan() { return plan; }
    public CastExecution execution() { return execution; }
    public TriggerMana billingSource() { return mana; }
    public CastSession billing(TriggerMana mana, boolean repeatPayments) {
        this.mana = mana; this.repeatPayments = repeatPayments; return this;
    }
    public boolean activate(java.util.function.BooleanSupplier action) {
        return !repeatPayments || mana == null ? action.getAsBoolean() : mana.trigger(plan.spellId(), plan.spellLevel(), action);
    }
    public CastAim aim() { return aim.sample(); }
    public State state() { return state; }
    public boolean active() { return state == State.ACTIVE; }
    public int remainingTicks() { return (int)Math.clamp(plan.maxTicks() - (world.getGameTime() - started), 0, Integer.MAX_VALUE); }
    public boolean permitted() {
        return caster.isAlive() && !caster.isRemoved() && caster.level() == world
                && SpellRegistry.getSpell(plan.spellId()).isEnabled()
                && GlyphAccessEvent.allowed(caster, plan.glyphId(), plan.spellId(), plan.spellLevel(), GlyphAccessEvent.Action.RESOLVE);
    }
    public boolean start() {
        if (state != State.PREPARED) return false;
        if (!permitted()) { finish(EndReason.DENIED); return false; }
        state = State.ACTIVE;
        boolean started = repeatPayments || mana == null ? execution.start(this)
                : mana.trigger(plan.spellId(), plan.spellLevel(), () -> execution.start(this));
        if (!started) { finish(EndReason.SPAWN_FAILED); return false; }
        return true;
    }
    public void tick() {
        if (!active()) return;
        if (!caster.isAlive() || caster.isRemoved() || caster.level() != world) { finish(EndReason.OWNER_GONE); return; }
        long age = world.getGameTime() - started;
        if (age >= plan.maxTicks()) { finish(EndReason.EXPIRED); return; }
        if (age % 20 == 0 && !permitted()) { finish(EndReason.DENIED); return; }
        if (execution.tick(this)) finish(EndReason.COMPLETED);
    }
    /** Called only after successful native damage, or a real block collision. Never charges mana. */
    public void impact(HitResult hit) {
        if (!active() || !permitted()) return;
        TriggerGeometry.scoped(execution.incomingDirection(), () -> continuation.resolve(hit));
    }
    public void finish(EndReason reason) {
        if (state == State.COMPLETED || state == State.CANCELLED) return;
        state = reason == EndReason.COMPLETED ? State.COMPLETED : State.CANCELLED;
        execution.close(this, reason);
        continuation.close();
    }
}
