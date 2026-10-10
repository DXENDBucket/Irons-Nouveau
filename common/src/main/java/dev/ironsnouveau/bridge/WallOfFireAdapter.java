package dev.ironsnouveau.bridge;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.api.execution.ExecutionSession;
import dev.arsconflux.api.interaction.*;
import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.api.InteractiveSpellAdapter;
import dev.ironsnouveau.casting.ActiveCooldowns;
import dev.ironsnouveau.casting.EffectResources;
import dev.ironsnouveau.mixin.WallOfFireAccess;
import io.redspace.ironsspellbooks.entity.spells.wall_of_fire.WallOfFireEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.ArrayList;
import java.util.List;

/** Iron-owned path semantics; Core only carries inputs, lifetime and the suspended Ars remainder. */
public final class WallOfFireAdapter implements InteractiveSpellAdapter {
    public static final WallOfFireAdapter INSTANCE = new WallOfFireAdapter();
    private static final int WINDOW = 40;
    private WallOfFireAdapter() {}
    @Override public boolean begin(Resolution ctx, HitResult hit, SpellResolver resolver) {
        if (ctx.context().ars() == null || InputSessions.waiting(ctx.caster())) return false;
        ItemStack tool = ctx.context().ars().getCasterTool();
        if (tool.isEmpty()) return false; // A detached/device source must explicitly provide an input binding.
        InteractionHand hand = null;
        for (var candidate : InteractionHand.values()) if (ItemStack.matches(tool, ctx.caster().getItemInHand(candidate))) {
            hand = candidate; break;
        }
        if (hand == null) return false;
        var continuation = ArsContinuation.capture(resolver);
        var driver = new Driver(ctx, continuation);
        HitResult first = hit instanceof EntityHitResult e && e.getEntity() == ctx.caster()
                ? driver.select(new CastInput(CastInput.Action.ACTIVATE, hand, ctx.caster().getEyePosition(), ctx.caster().getLookAngle(), null)) : hit;
        if (!driver.add(first)) { continuation.close(); return false; }
        var endpoint = new InputSession<>(ctx.world(), ctx.context(), ctx, WINDOW,
                InputBinding.held(ctx.caster(), hand, resolver.spell), driver, continuation,
                () -> ctx.spell().isEnabled() && GlyphAccessEvent.allowed(ctx.caster(), ctx.definition().glyphId(),
                        ctx.definition().spellId(), driver.level, GlyphAccessEvent.Action.RESOLVE),
                action -> dev.ironsnouveau.casting.PresetTools.scoped(ctx.context().ars(), () -> { action.run(); return null; }));
        if (!InputSessions.start(endpoint)) return false;
        driver.cooldown = ActiveCooldowns.deferCurrent(ctx.caster(), ctx.spell());
        resolver.spellContext.stop();
        driver.prompt();
        return true;
    }
    private static final class Driver implements InteractiveDriver<Resolution> {
        final Resolution ctx;
        final ArsContinuation continuation;
        final WallOfFireAccess access;
        final int level, count;
        final float length, damage;
        final List<Vec3> anchors = new ArrayList<>();
        double used;
        HitResult last;
        Runnable cooldown = () -> {};
        Driver(Resolution ctx, ArsContinuation continuation) {
            this.ctx = ctx; this.continuation = continuation; access = (WallOfFireAccess)ctx.spell();
            level = ctx.level(); count = ctx.spell().getRecastCount(level, ctx.caster());
            length = (float)Math.max(.1, access.ironsNouveau$length(level, ctx.caster())
                    + AugmentScaling.radius(ctx.stats().getAoeMultiplier()));
            damage = access.ironsNouveau$damage(level, ctx.caster());
        }
        public boolean start(ExecutionSession<Resolution> session) { return !anchors.isEmpty(); }
        public boolean tick(ExecutionSession<Resolution> session) { return false; }
        HitResult select(CastInput input) {
            if (input.hit() != null) return input.hit();
            return ctx.world().clip(new ClipContext(input.origin(), input.origin().add(input.direction().scale(20)),
                    ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, ctx.caster()));
        }
        boolean add(HitResult hit) {
            if (hit == null || !ctx.world().hasChunkAt(BlockPos.containing(hit.getLocation()))) return false;
            Vec3 next = access.ironsNouveau$ground(hit.getLocation(), ctx.world());
            double proposed = used;
            if (!anchors.isEmpty()) {
                Vec3 previous = anchors.get(anchors.size() - 1);
                double distance = previous.distanceTo(next), available = Math.max(0, length - used);
                if (distance > available) next = access.ironsNouveau$ground(previous.add(next.subtract(previous).normalize().scale(available)), ctx.world());
                proposed += Math.min(distance, available);
            }
            BlockPos pos = BlockPos.containing(next);
            if (!ctx.world().hasChunkAt(pos) || ctx.world().isOutsideBuildHeight(pos)
                    || !ctx.world().getWorldBorder().isWithinBounds(pos)) return false;
            used = proposed;
            anchors.add(next); last = new BlockHitResult(next, Direction.UP, BlockPos.containing(next), false);
            ctx.world().sendParticles(ParticleTypes.FLAME, next.x, next.y + 1.5, next.z, 5, .05, .25, .05, 0);
            return true;
        }
        public InputResult input(ExecutionSession<Resolution> session, CastInput input) {
            if (input.action() == CastInput.Action.CONFIRM) return InputResult.COMPLETED;
            if (!add(select(input))) return InputResult.REJECTED;
            if (anchors.size() >= count || used >= length) return InputResult.COMPLETED;
            session.renew(WINDOW); prompt(); return InputResult.ACCEPTED;
        }
        void prompt() {
            if (ctx.caster() instanceof Player player) player.displayClientMessage(
                    Component.translatable("irons_nouveau.cast.wall_select", Math.max(0, count - anchors.size())), true);
        }
        public void close(ExecutionSession<Resolution> session, ExecutionSession.EndReason reason) {
            if (reason == ExecutionSession.EndReason.SPAWN_FAILED) return;
            try {
                if (reason != ExecutionSession.EndReason.COMPLETED && reason != ExecutionSession.EndReason.EXPIRED) return;
                if (anchors.size() == 1) add(select(new CastInput(CastInput.Action.ACTIVATE, null,
                        ctx.caster().getEyePosition(), ctx.caster().getLookAngle(), null)));
                var wall = new WallOfFireEntity(ctx.world(), ctx.damageOwner(), List.copyOf(anchors), damage);
                Vec3 center = anchors.stream().reduce(Vec3.ZERO, Vec3::add).scale(1.0 / anchors.size());
                wall.setPos(center);
                // The native wall lives 240 ticks. Its attacks belong to this one paid activation.
                if (EffectResources.spawnOnce(ctx, wall, 241)) continuation.resolve(last);
            } finally { cooldown.run(); }
        }
    }
}
