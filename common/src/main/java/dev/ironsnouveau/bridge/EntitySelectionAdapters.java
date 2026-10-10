package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.casting.EffectResources;
import dev.ironsnouveau.casting.TelekinesisExecution;
import dev.ironsnouveau.casting.StepAimGeometry;
import dev.ironsnouveau.mixin.StepDistanceAccess;
import dev.ironsnouveau.platform.Casters;
import dev.ironsnouveau.platform.IronEvents;
import dev.ironsnouveau.platform.StepParticles;
import io.redspace.ironsspellbooks.api.events.SpellTeleportEvent;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.spells.ender.TeleportSpell;
import io.redspace.ironsspellbooks.entity.mobs.frozen_humanoid.FrozenHumanoid;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Selection supplies the recipient/destination, never replaces the original caster or payer. */
public final class EntitySelectionAdapters {
    private EntitySelectionAdapters() {}
    public static final LocationSpellAdapter BLOOD_STEP = (ctx, hit) -> step(ctx, hit, false);
    public static final LocationSpellAdapter FROST_STEP = (ctx, hit) -> step(ctx, hit, true);
    public static final LocationSpellAdapter TELEKINESIS = (ctx, hit) -> {
        var target = target(ctx, hit);
        return target != null && TelekinesisExecution.start(ctx, target);
    };
    public static LivingEntity target(Resolution ctx, HitResult hit) {
        var caster = ctx.caster();
        if (caster == null || Casters.isFake(caster) || !valid(ctx, caster)
                || !(hit instanceof EntityHitResult selected) || !(selected.getEntity() instanceof LivingEntity target)
                || target == caster || !valid(ctx, target)) return null;
        return target;
    }
    private static boolean valid(Resolution ctx, LivingEntity entity) {
        return entity.isAlive() && !entity.isRemoved() && !entity.isSpectator()
                && entity.level() == ctx.world() && ctx.world().hasChunkAt(entity.blockPosition());
    }
    private static boolean safe(Resolution ctx, Vec3 destination) {
        var box = ctx.caster().getBoundingBox().move(destination.subtract(ctx.caster().position()));
        return Double.isFinite(destination.x) && Double.isFinite(destination.y) && Double.isFinite(destination.z)
                && box.minY >= ctx.world().getMinBuildHeight() && box.maxY < ctx.world().getMaxBuildHeight()
                && ctx.world().getWorldBorder().isWithinBounds(box)
                && ctx.world().hasChunksAt(BlockPos.containing(box.minX, box.minY, box.minZ),
                        BlockPos.containing(box.maxX, box.maxY, box.maxZ))
                && ctx.world().noCollision(ctx.caster(), box);
    }
    private static Vec3 beside(Resolution ctx, LivingEntity target) {
        var caster = ctx.caster();
        Vec3 destination = null;
        double separation = Math.max(1.5, (target.getBbWidth() + caster.getBbWidth()) / 2 + .5);
        for (int i = 0; i < 8 && destination == null; i++) {
            var candidate = target.position().subtract(new Vec3(0, 0, separation)
                    .yRot((float)-Math.toRadians(target.getYRot() + i * 45)));
            for (int dy = 0; dy <= 1; dy++) {
                if (safe(ctx, candidate.add(0, dy, 0))) { destination = candidate.add(0, dy, 0); break; }
            }
        }
        return destination;
    }
    private static boolean step(Resolution ctx, HitResult hit, boolean frost) {
        var caster = ctx.caster();
        if (caster == null || Casters.isFake(caster) || !valid(ctx, caster)) return false;
        LivingEntity target = null;
        Vec3 destination;
        if (hit instanceof EntityHitResult selected && selected.getEntity() instanceof LivingEntity living && living != caster) {
            if (!valid(ctx, living)) return false;
            target = living;
            destination = beside(ctx, target);
        } else {
            float range = ((StepDistanceAccess)ctx.spell()).ironsNouveau$distance(ctx.level(), caster);
            if (!Float.isFinite(range) || range <= 0) return false;
            if (hit instanceof EntityHitResult selected && selected.getEntity() == caster) {
                var end = caster.getEyePosition().add(caster.getLookAngle().scale(range));
                if (!ctx.world().hasChunksAt(caster.blockPosition(), BlockPos.containing(end))) return false;
                // Native Blood Step can select the creature under the caster's crosshair; Frost Step casts toward terrain.
                if (!frost) {
                    var aimed = RaycastBuilder.begin(ctx.world(), caster).range(range).checkForBlocks(true).build();
                    if (aimed instanceof EntityHitResult entity && entity.getEntity() instanceof LivingEntity living && living != caster) {
                        if (!valid(ctx, living)) return false;
                        target = living;
                    }
                }
                destination = target != null ? beside(ctx, target) : TeleportSpell.findTeleportLocation(ctx.world(), caster, range);
            } else {
                if (hit instanceof EntityHitResult selected) {
                    var entity = selected.getEntity();
                    if (!entity.isAlive() || entity.isRemoved() || entity.isSpectator() || entity.level() != ctx.world()
                            || !ctx.world().hasChunkAt(entity.blockPosition())) return false;
                }
                destination = StepAimGeometry.towards(ctx, hit.getLocation(), range);
            }
        }
        if (destination == null || !safe(ctx, destination) || destination.distanceToSqr(caster.position()) < .0001) return false;
        var event = new SpellTeleportEvent(ctx.spell(), caster, destination.x, destination.y, destination.z);
        if (IronEvents.teleport(event)) return false;
        destination = new Vec3(event.getTargetX(), event.getTargetY(), event.getTargetZ());
        // Event listeners may redirect a teleport; validate the final destination before changing anything.
        if (!safe(ctx, destination)) return false;
        Vec3 origin = caster.position();
        FrozenHumanoid shadow = frost ? new FrozenHumanoid(ctx.world(), caster) : null;
        if (caster.isPassenger()) caster.stopRiding();
        caster.teleportTo(destination.x, destination.y, destination.z);
        caster.fallDistance = 0;
        if (target != null) caster.lookAt(EntityAnchorArgument.Anchor.EYES, target.getEyePosition().add(0, -.15, 0));
        if (frost) {
            shadow.setShatterDamage((float)ctx.power());
            shadow.setDeathTimer(EffectResources.ticks(100 * ctx.duration()));
            ctx.world().addFreshEntity(shadow);
            var lastAttacker = caster.getLastHurtByMob();
            Utils.performTaunt(shadow, 10, mob -> lastAttacker == null ? caster instanceof Enemy ^ mob instanceof Enemy
                    : mob.getClass().isAssignableFrom(lastAttacker.getClass()) || mob.isAlliedTo(lastAttacker)
                        || caster instanceof Enemy ^ mob instanceof Enemy);
            StepParticles.frost(caster, origin, destination);
        } else {
            caster.setInvisible(true);
            caster.addEffect(dev.ironsnouveau.platform.Effects.create(MobEffectRegistry.TRUE_INVISIBILITY,
                    EffectResources.ticks(100 * ctx.duration()), 0, false, false, true), caster);
            ctx.world().sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, origin.x, origin.y + 1, origin.z, 35, .3, .5, .3, .03);
        }
        return true;
    }
}
