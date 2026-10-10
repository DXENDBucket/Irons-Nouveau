package dev.ironsnouveau.casting;

import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.context.CastContexts;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.HitResult;
import java.util.UUID;

/** Iron parameter/authorization facade; Conflux owns the execution state and lifecycle. */
public final class CastSession {
    public enum State { PREPARED, ACTIVE, COMPLETED, CANCELLED }
    public enum EndReason { COMPLETED, EXPIRED, OWNER_GONE, DENIED, WORLD_UNLOADED, SPAWN_FAILED, SERVER_STOPPED, ERROR }
    private final CastPlan plan;
    private final AimSource aim;
    private final CastExecution execution;
    private final ExecutionSession<CastPlan> delegate;
    private TriggerMana mana;
    private final PresetTools.Token preset = PresetTools.current();
    public CastSession(ServerLevel world, LivingEntity caster, CastPlan plan, AimSource aim,
                       CastExecution execution, ImpactContinuation continuation) {
        this(world, legacyContext(caster), plan, aim, execution, continuation);
    }
    private static CastContext legacyContext(LivingEntity caster) {
        var current = CastContexts.current();
        return current != null && current.caster() == caster ? current : CastContext.detached(caster, null, null);
    }
    public CastSession(ServerLevel world, CastContext context, CastPlan plan, AimSource aim,
                       CastExecution execution, ImpactContinuation continuation) {
        var caster = context.caster();
        this.plan = plan; this.aim = aim; this.execution = execution;
        delegate = new ExecutionSession<>(world, context, plan, plan.maxTicks(), new ExecutionDriver<CastPlan>() {
            public boolean start(ExecutionSession<CastPlan> session) { return execution.start(CastSession.this); }
            public boolean tick(ExecutionSession<CastPlan> session) { return execution.tick(CastSession.this); }
            public void close(ExecutionSession<CastPlan> session, ExecutionSession.EndReason reason) {
                execution.close(CastSession.this, EndReason.valueOf(reason.name()));
            }
            public net.minecraft.world.phys.Vec3 incomingDirection() { return execution.incomingDirection(); }
            public boolean occupiesCaster() { return execution.occupiesCaster(); }
        }, new dev.arsconflux.api.execution.ImpactContinuation() {
            public void resolve(HitResult hit) { continuation.resolve(hit); }
            public void close() { continuation.close(); }
        }, () -> SpellRegistry.getSpell(plan.spellId()).isEnabled()
                && GlyphAccessEvent.allowed(caster, plan.glyphId(), plan.spellId(), plan.spellLevel(), GlyphAccessEvent.Action.RESOLVE),
                action -> PresetTools.scoped(preset, () -> { action.run(); return null; }));
    }
    ExecutionSession<CastPlan> coreSession() { return delegate; }
    public UUID id() { return delegate.id(); }
    public ServerLevel world() { return delegate.world(); }
    public LivingEntity caster() { return delegate.caster(); }
    public CastContext context() { return delegate.context(); }
    public LivingEntity executor() { return context().executor(); }
    public LivingEntity damageOwner() { return context().damageOwner(); }
    public CastPlan plan() { return plan; }
    public CastExecution execution() { return execution; }
    public TriggerMana billingSource() { return mana; }
    public CastSession billing(TriggerMana mana, boolean repeatPayments) {
        this.mana = mana;
        delegate.account(mana == null ? null : mana.account());
        delegate.billing(mana == null ? TriggerPayment.FREE : action -> mana.trigger(plan.spellId(), plan.spellLevel(), action), repeatPayments);
        return this;
    }
    public boolean activate(java.util.function.BooleanSupplier action) { return delegate.activate(action); }
    public CastAim aim() { return aim.sample(); }
    public State state() { return State.valueOf(delegate.state().name()); }
    public boolean active() { return delegate.active(); }
    public int remainingTicks() { return delegate.remainingTicks(); }
    public boolean permitted() { return delegate.permitted(); }
    public boolean start() { return delegate.start(); }
    public void tick() { delegate.tick(); }
    public void impact(HitResult hit) { delegate.impact(hit); }
    public void finish(EndReason reason) { delegate.finish(ExecutionSession.EndReason.valueOf(reason.name())); }
}
