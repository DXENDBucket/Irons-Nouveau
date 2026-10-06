package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.casting.MotionEffects;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.player.SpinAttackType;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import java.util.Comparator;
import java.util.function.BiPredicate;

/** Entity-only actions: hit recipient moves; the original caster pays and supplies offensive stats. */
public final class MotionAdapters {
    private MotionAdapters() {}
    private static LocationSpellAdapter selected(BiPredicate<Resolution, LivingEntity> action) {
        return (ctx, hit) -> hit instanceof EntityHitResult entity && entity.getEntity() instanceof LivingEntity actor
                && actor.isAlive() && !actor.isSpectator() && !actor.isPassenger() && actor.level() == ctx.world()
                && ctx.world().hasChunkAt(actor.blockPosition()) && action.test(ctx, actor);
    }
    public static final LocationSpellAdapter BURNING_DASH = selected((ctx, actor) -> dash(ctx, actor, false));
    public static final LocationSpellAdapter VOLT_STRIKE = selected((ctx, actor) -> dash(ctx, actor, true));
    public static final LocationSpellAdapter SHADOW_SLASH = selected(MotionAdapters::slash);
    public static final LocationSpellAdapter ASCENSION = selected(MotionAdapters::ascend);

    private static void sync(LivingEntity actor) {
        actor.hurtMarked = true; actor.hasImpulse = true;
        if (actor instanceof ServerPlayer player) player.connection.send(new ClientboundSetEntityMotionPacket(actor));
    }
    private static boolean enemy(Resolution ctx, LivingEntity actor, Entity target) {
        return target != actor && target != ctx.caster() && target.isAlive() && !target.isSpectator()
                && !ctx.caster().isAlliedTo(target);
    }
    private static boolean dash(Resolution ctx, LivingEntity actor, boolean lightning) {
        if (actor.hasEffect(MobEffectRegistry.BURNING_DASH.get()) || actor.hasEffect(MobEffectRegistry.VOLT_STRIKE.get())) return false;
        int ticks = lightning ? 10 : 15;
        float damage = (float)(5 + ctx.power());
        int amplifier = (int)net.minecraft.util.Mth.clamp(damage, 1, 255);
        var effect = lightning ? MobEffectRegistry.VOLT_STRIKE.get() : MobEffectRegistry.BURNING_DASH.get();
        if (!actor.addEffect(new MobEffectInstance(effect, ticks, amplifier, false, false, false), ctx.caster())) return false;
        MotionEffects.bind(ctx, actor, ticks, damage, amplifier);
        Vec3 look = actor.getLookAngle(), impulse;
        if (lightning) {
            impulse = look.scale(3 * (15 + ctx.power()) / 20);
            impulse = impulse.multiply(1, 1 - Math.max(look.y, 0) * .6, 1);
        } else impulse = look.multiply(3, 1, 3).normalize().add(0, .25, 0).scale((15 + ctx.power()) / 12);
        // Collision-aware clearance, followed by position and velocity synchronization for player recipients.
        if (actor.onGround()) {
            actor.move(MoverType.SELF, new Vec3(0, lightning ? 1 : 1.5, 0));
            if (actor instanceof ServerPlayer player)
                player.connection.teleport(actor.getX(), actor.getY(), actor.getZ(), actor.getYRot(), actor.getXRot());
        }
        actor.setDeltaMovement(actor.getDeltaMovement().lerp(impulse, .75));
        actor.invulnerableTime = 20;
        MagicData.getPlayerMagicData(actor).getSyncedData().setSpinAttackType(lightning ? SpinAttackType.LIGHTNING : SpinAttackType.FIRE);
        sync(actor); return true;
    }
    private static boolean slash(Resolution ctx, LivingEntity actor) {
        Vec3 forward = actor.getLookAngle(), start = actor.getEyePosition();
        Vec3 end = Utils.raycastForBlock(ctx.world(), start, start.add(forward.scale(12)), ClipContext.Fluid.NONE).getLocation();
        var candidates = ctx.world().getEntities(actor, actor.getBoundingBox().expandTowards(end.subtract(start)).inflate(2),
                e -> enemy(ctx, actor, e) && (e instanceof LivingEntity || e instanceof Projectile)
                        && e.getBoundingBox().getCenter().subtract(actor.getBoundingBox().getCenter()).normalize().dot(forward) >= .85);
        var nearest = candidates.stream().min(Comparator.comparingDouble(actor::distanceToSqr));
        if (nearest.isPresent() && actor.distanceToSqr(nearest.get()) < 144) {
            var box = AABB.ofSize(nearest.get().getBoundingBox().getCenter(), 2.5, 3.5, 2.5).move(forward.scale(1.25));
            end = box.getCenter().add(end).scale(.5);
            var source = ctx.spell().getDamageSource(actor, ctx.caster());
            for (var target : ctx.world().getEntities(actor, box, e -> enemy(ctx, actor, e))) {
                if (!Utils.hasLineOfSight(ctx.world(), start, target.getBoundingBox().getCenter(), true)) continue;
                if (target instanceof Projectile projectile && !projectile.noPhysics && !projectile.getType().is(ModTags.CANT_PARRY)
                        && !(projectile instanceof AbstractArrow arrow && ((io.redspace.ironsspellbooks.mixin.AbstractArrowAccessor)arrow).isInGround())) {
                    projectile.setOwner(ctx.caster());
                    projectile.shoot(forward.x, forward.y, forward.z, (float)projectile.getDeltaMovement().length(), 0);
                } else if (DamageSources.applyDamage(target, (float)ctx.power() + WeaponStats.damage(ctx.caster()), source)) {
                    EnchantmentHelper.doPostDamageEffects(ctx.caster(), target);
                    target.setDeltaMovement(target.getDeltaMovement().add(target.position().subtract(actor.position()).normalize().add(0, .5, 0).normalize()));
                    target.hurtMarked = true;
                }
            }
        }
        var path = end.subtract(start);
        var direction = path.normalize();
        var up = Math.abs(direction.y) > .999 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        var right = up.cross(direction).normalize();
        var slash = new io.redspace.ironsspellbooks.particle.EnderSlashParticleOptions((float)direction.x, (float)direction.y,
                (float)direction.z, (float)right.x, (float)right.y, (float)right.z, 1);
        var visual = end.subtract(direction.scale(3)).add(right.scale(-.3));
        ctx.world().sendParticles(slash, visual.x, visual.y + .3, visual.z, 1, 0, 0, 0, 0);
        actor.setDeltaMovement(actor.getDeltaMovement().scale(.2).add(path.scale(1.0 / 6)).add(0, .1, 0));
        actor.addEffect(new MobEffectInstance(MobEffectRegistry.FALL_DAMAGE_IMMUNITY.get(), 20), ctx.caster());
        for (int i = 0; i < 15; i++) {
            var pos = start.add(path.scale(i / 15.0));
            ctx.world().sendParticles(ParticleTypes.PORTAL, pos.x, pos.y, pos.z, 2, .2, .2, .2, .1);
        }
        sync(actor); return true;
    }
    private static boolean ascend(Resolution ctx, LivingEntity actor) {
        var point = actor.position();
        for (int i = 0; i < 32 && ctx.world().getBlockState(BlockPos.containing(point).below()).isAir(); i++) point = point.add(0, -1, 0);
        var bolt = EntityType.LIGHTNING_BOLT.create(ctx.world());
        if (bolt == null) return false;
        bolt.setVisualOnly(true); bolt.setDamage(0); bolt.setPos(point);
        if (!ctx.world().addFreshEntity(bolt)) return false;
        double radius = WorldSpellAdapters.radius(ctx, 5);
        var source = ctx.spell().getDamageSource(bolt, ctx.caster());
        for (var target : ctx.world().getEntities(actor, actor.getBoundingBox().inflate(radius), e -> enemy(ctx, actor, e))) {
            double fraction = 1 - target.distanceToSqr(point) / (radius * radius);
            if (fraction <= 0) continue;
            float damage = (float)(ctx.power() * fraction);
            DamageSources.applyDamage(target, damage, source);
            if (target instanceof Creeper creeper) creeper.thunderHit(ctx.world(), bolt);
            if (target instanceof LivingEntity living) living.knockback(.25 + damage / 10, actor.getX() - target.getX(), actor.getZ() - target.getZ());
        }
        actor.addEffect(new MobEffectInstance(MobEffectRegistry.ASCENSION.get(), 80), ctx.caster());
        actor.setDeltaMovement(actor.getDeltaMovement().add(actor.getLookAngle().multiply(1, 0, 1).normalize().add(0, 5, 0).scale(.125)));
        sync(actor); return true;
    }
}
