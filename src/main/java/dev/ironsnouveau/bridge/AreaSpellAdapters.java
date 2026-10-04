package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.BlizzardAoe;
import io.redspace.ironsspellbooks.entity.spells.EarthquakeAoe;
import io.redspace.ironsspellbooks.entity.spells.magma_ball.FireField;
import io.redspace.ironsspellbooks.entity.spells.black_hole.BlackHole;
import io.redspace.ironsspellbooks.entity.spells.poison_cloud.PoisonSplash;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.spells.fire.HeatSurgeSpell;
import io.redspace.ironsspellbooks.spells.ice.FrostwaveSpell;
import io.redspace.ironsspellbooks.spells.lightning.ShockwaveSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.phys.*;
import java.util.List;
import java.util.function.BiConsumer;

/** Point-centered areas share target filtering, while their native entities retain pulse behavior. */
public final class AreaSpellAdapters {
    private AreaSpellAdapters() {}
    static Vec3 direction(Resolution ctx, HitResult hit) {
        return dev.ironsnouveau.casting.BreathPose.from(ctx, hit).sample().direction();
    }
    static boolean loaded(Resolution ctx, HitResult hit) {
        return hit.getType() != HitResult.Type.MISS && ctx.world().hasChunkAt(BlockPos.containing(WorldSpellAdapters.center(hit)));
    }
    static List<LivingEntity> targets(Resolution ctx, Vec3 center, double radius) {
        return ctx.world().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius), target ->
                target != ctx.caster() && target.isAlive() && target.isPickable() && !target.isSpectator()
                        && !DamageSources.isFriendlyFireBetween(target, ctx.caster())
                        && target.distanceToSqr(center) < radius * radius
                        && Utils.hasLineOfSight(ctx.world(), center.add(0, 1, 0), target.getBoundingBox().getCenter(), true));
    }
    public static final LocationSpellAdapter EARTHQUAKE = (ctx, hit) -> {
        if (!loaded(ctx, hit)) return false;
        var field = new EarthquakeAoe(ctx.world()); field.moveTo(WorldSpellAdapters.center(hit)); field.setOwner(ctx.caster()); field.setCircular();
        int ticks = EffectResources.ticks(240 * ctx.duration());
        float damage = (float)ctx.power() * .25f;
        field.setRadius((float)WorldSpellAdapters.radius(ctx, 4 + 4 * ctx.spell().getEntityPowerMultiplier(ctx.caster())));
        field.setDuration(ticks); field.setDamage(damage); field.setSlownessAmplifier(Math.clamp((int)damage - 2, 0, 2));
        return EffectResources.spawn(ctx, field, ticks);
    };
    public static final LocationSpellAdapter BLIZZARD = (ctx, hit) -> {
        if (!loaded(ctx, hit)) return false;
        var field = new BlizzardAoe(EntityRegistry.BLIZZARD_AOE.get(), ctx.world());
        field.moveTo(WorldSpellAdapters.center(hit)); field.setOwner(ctx.caster());
        int ticks = EffectResources.ticks(20 * (10 + 1.5 * ctx.level()) * ctx.duration());
        field.setRadius((float)WorldSpellAdapters.radius(ctx, 2 + 6 * ctx.spell().getEntityPowerMultiplier(ctx.caster())));
        field.setDuration(ticks); field.setDeltaMovement(direction(ctx, hit).multiply(1, 0, 1).normalize().scale(.05));
        return EffectResources.spawn(ctx, field, ticks);
    };
    public static final LocationSpellAdapter POISON_SPLASH = (ctx, hit) -> {
        if (!loaded(ctx, hit)) return false;
        var splash = new PoisonSplash(ctx.world()); splash.setOwner(ctx.caster()); splash.moveTo(WorldSpellAdapters.center(hit));
        splash.setDamage((float)ctx.power()); splash.setEffectDuration(EffectResources.ticks((100.0 + ctx.level() * 40.0) * ctx.duration()));
        return EffectResources.spawnOnce(ctx, splash, 20);
    };
    public static final LocationSpellAdapter GRAVITY_FISSURE = (ctx, hit) -> {
        if (!loaded(ctx, hit)) return false;
        var hole = new BlackHole(ctx.world(), ctx.caster()); hole.moveTo(WorldSpellAdapters.center(hit));
        int ticks = EffectResources.ticks(ctx.power() * 20 * ctx.duration());
        hole.setRadius((float)WorldSpellAdapters.radius(ctx, 3.5)); hole.setDamage(0); hole.setDuration(ticks);
        hole.setDeltaMovement(direction(ctx, hit).scale(.2)); return EffectResources.spawn(ctx, hole, ticks);
    };
    public static final LocationSpellAdapter SCORCH = (ctx, hit) -> {
        if (!loaded(ctx, hit)) return false;
        Vec3 center = WorldSpellAdapters.center(hit); double radius = WorldSpellAdapters.radius(ctx, 2.5);
        var field = new FireField(ctx.world()); field.setOwner(ctx.caster()); field.moveTo(center);
        int ticks = EffectResources.ticks(200 * ctx.duration());
        field.setDuration(ticks); field.setDamage((float)ctx.power() * .1f); field.setRadius((float)radius); field.setCircular();
        if (!EffectResources.spawn(ctx, field, ticks)) return false;
        for (var target : targets(ctx, center, radius)) {
            DamageSources.applyDamage(target, (float)ctx.power(), ctx.spell().getDamageSource(ctx.caster()));
            DamageSources.ignoreNextKnockback(target);
        }
        ctx.world().sendParticles(ParticleTypes.LAVA, center.x, center.y, center.z, 25, 1, 1, 1, 1); return true;
    };
    private static boolean wave(Resolution ctx, HitResult hit, double baseRadius, BiConsumer<Resolution, LivingEntity> effect) {
        if (!loaded(ctx, hit)) return false;
        Vec3 center = WorldSpellAdapters.center(hit); double radius = WorldSpellAdapters.radius(ctx, baseRadius);
        for (var target : targets(ctx, center, radius)) effect.accept(ctx, target);
        io.redspace.ironsspellbooks.capabilities.magic.MagicManager.spawnParticles(ctx.world(),
                new io.redspace.ironsspellbooks.particle.BlastwaveParticleOptions(ctx.spell().getSchoolType().getTargetingColor(), (float)radius),
                center.x, center.y + .165, center.z, 1, 0, 0, 0, 0, true);
        return true;
    }
    public static final LocationSpellAdapter HEAT_SURGE = (ctx, hit) -> wave(ctx, hit,
            ((HeatSurgeSpell)ctx.spell()).getRadius(ctx.level(), ctx.caster()), (c, target) -> {
                var spell = (HeatSurgeSpell)c.spell();
                int ticks = EffectResources.ticks(spell.getDuration(c.level(), c.caster()) * c.duration());
                target.addEffect(new MobEffectInstance(MobEffectRegistry.REND, ticks, Math.clamp(spell.getRendAmplifier(c.level(), c.caster()), 0, 255)), c.caster());
                target.setRemainingFireTicks(Math.min(ticks / 2, 160));
            });
    public static final LocationSpellAdapter FROSTWAVE = (ctx, hit) -> wave(ctx, hit,
            ((FrostwaveSpell)ctx.spell()).getRadius(ctx.level(), ctx.caster()), (c, target) ->
                target.addEffect(new MobEffectInstance(MobEffectRegistry.CHILLED,
                        EffectResources.ticks(((FrostwaveSpell)c.spell()).getDuration(c.level(), c.caster()) * c.duration())), c.caster()));
    public static final LocationSpellAdapter SHOCKWAVE = (ctx, hit) -> wave(ctx, hit,
            ((ShockwaveSpell)ctx.spell()).getRadius(ctx.level(), ctx.caster()), (c, target) -> {
                DamageSources.applyDamage(target, ((ShockwaveSpell)c.spell()).getDamage(c.level(), c.caster()), c.spell().getDamageSource(c.caster()));
                if (target instanceof Creeper creeper) {
                    var bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(c.world());
                    if (bolt != null) { bolt.setVisualOnly(true); bolt.setDamage(0); creeper.thunderHit(c.world(), bolt); }
                }
            });
}
