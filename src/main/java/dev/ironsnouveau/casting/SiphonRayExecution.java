package dev.ironsnouveau.casting;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.network.particles.BloodSiphonParticlesPacket;
import io.redspace.ironsspellbooks.spells.blood.RayOfSiphoningSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.network.PacketDistributor;

/** Native range, damage, lifesteal, particles and per-pulse cost, with a breath-style pose. */
public final class SiphonRayExecution implements CastExecution {
    private final BreathPose pose;
    private long nextPulse;
    public SiphonRayExecution(BreathPose pose) { this.pose = pose; }
    @Override public boolean start(CastSession session) {
        if (!pose.valid(session)) return false;
        pulse(session); // The enclosing BridgeGlyph reserved and bills this first pulse.
        nextPulse = session.world().getGameTime() + 10;
        MovementRestrictions.begin(pose.anchor(), session.id(), session.remainingTicks());
        return true;
    }
    @Override public boolean tick(CastSession session) {
        if (!pose.valid(session) || !session.permitted()
                || session.billingSource().affordableCount(session.plan().spellId(), session.plan().spellLevel(), 1) == 0) return true;
        if (session.world().getGameTime() >= nextPulse) {
            if (!session.activate(() -> { pulse(session); return true; })) return true;
            nextPulse = session.world().getGameTime() + 10;
        }
        return false;
    }
    private void pulse(CastSession session) {
        var aim = pose.sample();
        var end = aim.origin().add(aim.direction().scale(RayOfSiphoningSpell.getRange(session.plan().spellLevel())));
        if (!session.world().hasChunksAt(BlockPos.containing(aim.origin()), BlockPos.containing(end))) return;
        var hit = RaycastBuilder.begin(session.world(), session.caster()).start(aim.origin()).end(end)
                .checkForBlocks(true).bbInflation(.15f)
                .filter(entity -> entity != session.caster() && entity != pose.anchor() && entity.canBeHitByProjectile()).build();
        if (!(hit instanceof EntityHitResult eh)) return;
        var spell = SpellRegistry.getSpell(session.plan().spellId());
        float damage = .25f * session.plan().nativePower();
        if (DamageSources.applyDamage(eh.getEntity(), damage, spell.getDamageSource(session.caster()))) {
            PacketDistributor.sendToPlayersTrackingEntityAndSelf(session.caster(), new BloodSiphonParticlesPacket(
                    eh.getEntity().position().add(0, eh.getEntity().getBbHeight() * .5, 0), aim.origin()));
        }
        spell.getCastFinishSound().ifPresent(sound -> session.world().playSound(null, aim.origin().x, aim.origin().y,
                aim.origin().z, sound, SoundSource.PLAYERS, .7f, 1f));
    }
    @Override public void close(CastSession session, CastSession.EndReason reason) {
        MovementRestrictions.end(pose.anchor(), session.id());
    }
}
