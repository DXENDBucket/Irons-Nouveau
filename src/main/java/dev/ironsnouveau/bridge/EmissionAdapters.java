package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.casting.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.entity.spells.*;
import io.redspace.ironsspellbooks.entity.spells.comet.Comet;
import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import io.redspace.ironsspellbooks.registries.EntityRegistry;
import io.redspace.ironsspellbooks.spells.evocation.ChainCreeperSpell;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Native emitters and scheduled bursts use Ars hit positions and one common resource account. */
public final class EmissionAdapters {
    private EmissionAdapters() {}
    public static final LocationSpellAdapter STARFALL = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        float radius = (float)WorldSpellAdapters.radius(ctx, 6);
        int duration = EffectResources.ticks(ctx.spell().getCastTime(ctx.level()) * ctx.duration());
        return TimedEffects.start(ctx, hit, 4, duration, false, (session, aim) -> {
            Vec3 center = aim.origin();
            var nearby = ctx.world().getEntitiesOfClass(LivingEntity.class, new AABB(center, center).inflate(radius),
                    e -> e != ctx.caster() && e.isAlive() && !io.redspace.ironsspellbooks.damage.DamageSources.isFriendlyFireBetween(e, ctx.caster()));
            Vec3 weighted = Vec3.ZERO;
            for (var entity : nearby) weighted = weighted.add(entity.position().subtract(center).scale(1.0 / nearby.size()));
            Vec3 offset = weighted.scale(.5); boolean any = false;
            for (int i = 0; i < 2; i++) {
                double angle = ctx.world().random.nextDouble() * Math.PI * 2;
                double r = ctx.world().random.nextDouble() * radius;
                Vec3 target = center.add(offset).add(Math.cos(angle) * r, .5, Math.sin(angle) * r);
                if (!ctx.world().hasChunksAt(BlockPos.containing(target), BlockPos.containing(target.add(0, 12, 0)))) continue;
                Vec3 direction = new Vec3(.15, -.85, 0).normalize();
                Vec3 start = Utils.raycastForBlock(ctx.world(), target, target.subtract(direction.scale(12)), ClipContext.Fluid.NONE).getLocation().add(direction);
                var comet = new Comet(ctx.world(), ctx.caster()); comet.moveTo(start);
                comet.shoot(direction, .075f); comet.setDamage(session.plan().nativePower() * .5f); comet.setExplosionRadius(2);
                any |= EffectResources.paidChildren(session, () -> ctx.world().addFreshEntity(comet));
            }
            return any;
        });
    };
    public static final LocationSpellAdapter BLAZE_STORM = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        int duration = EffectResources.ticks((55.0 + 5.0 * ctx.level()) * ctx.duration());
        return TimedEffects.start(ctx, hit, 5, duration, true, (session, aim) -> {
            var fireball = new SmallMagicFireball(ctx.world(), ctx.caster());
            fireball.moveTo(aim.origin().add(aim.direction().scale(.4)));
            fireball.shoot(aim.direction(), .05f); fireball.setDamage(session.plan().nativePower() * .4f);
            return EffectResources.paidChildren(session, () -> ctx.world().addFreshEntity(fireball));
        });
    };
    public static final LocationSpellAdapter CLOUD = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        double radius = WorldSpellAdapters.radius(ctx, 5);
        int duration = EffectResources.ticks(ctx.spell().getCastTime(ctx.level()) * ctx.duration());
        return TimedEffects.start(ctx, hit, 10, duration, true, (session, aim) -> {
            Vec3 pos = aim.origin();
            for (var target : ctx.world().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(radius),
                    e -> e.isAlive() && e.distanceToSqr(pos) <= radius * radius && Utils.shouldHealEntity(ctx.caster(), e))) {
                float healing = session.plan().nativePower() * .5f;
                net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new io.redspace.ironsspellbooks.api.events.SpellHealEvent(
                        ctx.caster(), target, healing, ctx.spell().getSchoolType()));
                target.heal(healing);
                ctx.world().sendParticles(ParticleTypes.HAPPY_VILLAGER, target.getX(), target.getY() + 1, target.getZ(), 4, .3, .3, .3, 0);
            }
            ctx.world().sendParticles(ParticleTypes.CLOUD, pos.x, pos.y + .3, pos.z, 12, radius * .6, .1, radius * .6, 0);
            return true;
        });
    };
    public static final LocationSpellAdapter ARROWS = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        var center = WorldSpellAdapters.center(hit);
        var forward = AreaSpellAdapters.direction(ctx, hit).multiply(1, 0, 1).normalize();
        if (forward.lengthSqr() < 1e-8) forward = new Vec3(0, 0, 1);
        var endpoint = center.add(forward.scale(-4)).add(0, 6, 0);
        if (!ctx.world().hasChunkAt(BlockPos.containing(endpoint))) return false;
        var spawn = ctx.world().clip(new ClipContext(center, endpoint, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, ctx.caster())).getLocation();
        var field = new ArrowVolleyEntity(EntityRegistry.ARROW_VOLLEY_ENTITY.get(), ctx.world());
        field.moveTo(spawn); field.setOwner(ctx.caster());
        var rotation = Utils.rotationFromDirection(center.subtract(spawn).normalize());
        field.setYRot((float)-Math.toDegrees(rotation.y));
        field.setXRot(Math.min(89, (float)-Math.toDegrees(rotation.x) + 25));
        int rows = (int)Math.min(32L, 4L + ctx.level());
        field.setRows(rows); field.setArrowsPerRow((int)Math.min(32L, 5L + ctx.level() / 2));
        field.setDamage((float)ctx.power() * .25f);
        return EffectResources.spawn(ctx, field, (rows + 1) * 5 + 2);
    };
    public static final LocationSpellAdapter FANG_SWIRL = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        var center = Utils.moveToRelativeGroundLevel(ctx.world(), WorldSpellAdapters.center(hit), 6);
        var field = new FangSwirlEntity(EntityRegistry.FANG_SWIRL.get(), ctx.world());
        field.moveTo(center); field.setStartPos(center); field.setDelay(0); field.setOwner(ctx.caster());
        int ticks = EffectResources.ticks(160 * ctx.duration());
        field.setDuration(ticks); field.setRadius((float)WorldSpellAdapters.radius(ctx, 4.5 + .5 * ctx.level() * ctx.spell().getEntityPowerMultiplier(ctx.caster())));
        field.setDamage((float)ctx.power() * .75f);
        return EffectResources.spawn(ctx, field, ticks + 1);
    };
    public static final LocationSpellAdapter CREEPERS = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        int count = (int)Math.min(64L, 2L + ctx.level());
        Vec3 center = WorldSpellAdapters.center(hit).add(0, .5, 0);
        return EffectResources.paidChildren(ctx, 300, () -> {
            ChainCreeperSpell.summonCreeperRing(ctx.world(), ctx.caster(), center, (float)ctx.power(), count);
            return true;
        });
    };
}
