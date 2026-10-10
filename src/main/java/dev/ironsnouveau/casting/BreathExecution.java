package dev.ironsnouveau.casting;

import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.entity.spells.AbstractShieldEntity;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.phys.*;

/** A paid ten-tick emission window. Failed upkeep ends this session permanently. */
public final class BreathExecution implements CastExecution {
    private final AbstractConeProjectile cone;
    private final BreathPose pose;
    private final float scale;
    private long nextPulse;
    private int pulses;
    public BreathExecution(AbstractConeProjectile cone, BreathPose pose, float scale) { this.cone = cone; this.pose = pose; this.scale = scale; }
    public AbstractConeProjectile cone() { return cone; }
    @Override public boolean occupiesCaster() { return true; }
    @Override public boolean start(CastSession session) {
        if (!pose.valid(session)) return false;
        var aim = pose.sample();
        if (!session.world().hasChunkAt(BlockPos.containing(aim.origin()))) return false;
        cone.setOwner(session.caster()); cone.setDamage(session.plan().nativePower());
        ((ConeState)cone).ironsNouveau$bind(pose.anchor() == null ? -1 : pose.anchor().getId(), pose.offset(), scale);
        BreathVisuals.place(cone, aim.origin(), aim.direction()); cone.setOldPosAndRot();
        if (!session.world().addFreshEntity(cone)) return false;
        // BridgeGlyph has reserved this first emission; do not bill it a second time.
        pulse(session);
        nextPulse = session.world().getGameTime() + 10;
        MovementRestrictions.begin(pose.anchor(), session.id(), session.remainingTicks());
        return true;
    }
    @Override public boolean tick(CastSession session) {
        if (cone.isRemoved() || !pose.valid(session)) return true;
        if (session.billingSource().affordableCount(session.plan().spellId(), session.plan().spellLevel(), 1) == 0) return true;
        var aim = pose.sample();
        if (!session.world().hasChunkAt(BlockPos.containing(aim.origin())) || !session.permitted()) return true;
        BreathVisuals.place(cone, aim.origin(), aim.direction());
        long now = session.world().getGameTime();
        if (now >= nextPulse) {
            if (!session.activate(() -> { pulse(session); return true; })) return true;
            nextPulse = now + 10;
        }
        return false;
    }
    private void pulse(CastSession session) {
        EffectResources.scoped(session, () -> {
            if (cone instanceof FireBreathProjectile && ServerConfigs.SPELL_GREIFING.get()) ignite(session);
            for (var target : ((ConeState)cone).ironsNouveau$targets()) {
                if (target != pose.anchor() && target.isAlive()) ((ConeState)cone).ironsNouveau$hit(new EntityHitResult(target));
            }
            return true;
        });
        if (pulses > 0 && pulses % 2 == 0)
            SpellRegistry.getSpell(session.plan().spellId()).getCastFinishSound().ifPresent(sound -> session.world().playSound(null,
                    cone.getX(), cone.getY(), cone.getZ(), sound, SoundSource.PLAYERS, .7f, 1f));
        pulses++;
    }
    private void ignite(CastSession session) {
        var world = session.world();
        var origin = pose.eyeAnchored() && pose.anchor() != null ? pose.anchor().getEyePosition() : cone.position();
        var random = world.random;
        for (int i = 0; i < 3; i++) {
            var direction = cone.getLookAngle().xRot((random.nextFloat() * 2 - 1) * .2617994f)
                    .yRot((random.nextFloat() * 2 - 1) * .2617994f);
            var end = origin.add(direction.scale(10 * scale));
            if (!world.hasChunksAt(BlockPos.containing(origin), BlockPos.containing(end))) continue;
            var hit = world.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, cone));
            if (hit.getType() != HitResult.Type.BLOCK) continue;
            var shield = RaycastBuilder.begin(world, cone).start(origin).end(hit.getLocation()).checkForBlocks(false)
                    .filter(e -> e.getClass() == AbstractShieldEntity.class).build();
            if (shield.getType() != HitResult.Type.MISS) continue;
            var block = BlockPos.containing(hit.getLocation().subtract(direction.scale(.5)));
            if (world.hasChunkAt(block) && world.getBlockState(block).isAir()) world.setBlockAndUpdate(block, BaseFireBlock.getState(world, block));
        }
    }
    @Override public void close(CastSession session, CastSession.EndReason reason) {
        MovementRestrictions.end(pose.anchor(), session.id());
        cone.discard();
    }
}
