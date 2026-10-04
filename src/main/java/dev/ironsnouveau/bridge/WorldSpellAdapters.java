package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.api.SpellAdapter;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.ChainLightning;
import io.redspace.ironsspellbooks.entity.spells.HealingAoe;
import io.redspace.ironsspellbooks.entity.spells.black_hole.BlackHole;
import io.redspace.ironsspellbooks.entity.spells.void_tentacle.VoidTentacle;
import io.redspace.ironsspellbooks.entity.spells.root.RootEntity;
import io.redspace.ironsspellbooks.entity.spells.wisp.WispEntity;
import io.redspace.ironsspellbooks.entity.spells.firefly_swarm.FireflySwarmProjectile;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.spells.nature.RootSpell;
import io.redspace.ironsspellbooks.spells.lightning.ChainLightningSpell;
import io.redspace.ironsspellbooks.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.phys.*;

/** Point effects run once per Ars impact, with native entities owning their internal pulses. */
public final class WorldSpellAdapters {
    private WorldSpellAdapters() {}
    public static Vec3 center(HitResult hit) {
        return hit instanceof EntityHitResult e ? e.getEntity().position() : hit.getLocation();
    }
    public static double radius(Resolution ctx, double base) { return Math.clamp(base + AugmentScaling.radius(ctx.stats().getAoeMultiplier()), .1, 48); }
    private static int duration(Resolution ctx, int base) { return EffectResources.ticks(base * ctx.duration()); }
    private static LivingEntity target(Resolution ctx, HitResult hit) {
        if (hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity living && living.isAlive()
                && living != ctx.caster() && !ctx.caster().isAlliedTo(living)) return living;
        return null;
    }
    public static final LocationSpellAdapter ELDRITCH_BLAST = (ctx, hit) -> {
        if (hit.getType() == HitResult.Type.MISS) return false;
        var visual = new io.redspace.ironsspellbooks.entity.spells.eldritch_blast.EldritchBlastVisualEntity(
                ctx.world(), ctx.caster().getEyePosition().add(0, -.75, 0), hit.getLocation(), ctx.caster());
        if (!EffectResources.spawn(ctx, visual, 40)) return false;
        var victim = target(ctx, hit);
        if (victim != null) DamageSources.applyDamage(victim, (float)ctx.power(), ctx.spell().getDamageSource(ctx.caster()));
        return true;
    };
    public static final LocationSpellAdapter LIGHTNING = (ctx, hit) -> {
        if (hit.getType() == HitResult.Type.MISS) return false;
        Vec3 pos = center(hit); if (!ctx.world().hasChunkAt(BlockPos.containing(pos))) return false;
        var bolt = EntityType.LIGHTNING_BOLT.create(ctx.world());
        if (bolt == null) return false;
        bolt.setVisualOnly(true); bolt.setDamage(0); bolt.setPos(pos);
        if (!ctx.world().addFreshEntity(bolt)) return false;
        double radius = radius(ctx, 4);
        for (var living : ctx.world().getEntitiesOfClass(LivingEntity.class, new AABB(pos, pos).inflate(radius))) {
            double distance = living.distanceToSqr(pos);
            if (living == ctx.caster() || ctx.caster().isAlliedTo(living) || distance >= radius * radius
                    || !Utils.hasLineOfSight(ctx.world(), pos.add(0, 2, 0), living.getBoundingBox().getCenter(), true)) continue;
            DamageSources.applyDamage(living, (float)(ctx.power() * (1 - distance / (radius * radius))), ctx.spell().getDamageSource(bolt, ctx.caster()));
            if (living instanceof Creeper creeper) creeper.thunderHit(ctx.world(), bolt);
        }
        return true;
    };
    public static final LocationSpellAdapter CHAIN = (ctx, hit) -> {
        var target = target(ctx, hit); if (target == null) return false;
        var spell = (ChainLightningSpell)ctx.spell();
        var chain = new ChainLightning(ctx.world(), ctx.caster(), target);
        chain.setDamage((float)ctx.power());
        chain.range = (float)radius(ctx, spell.getRange(ctx.level(), ctx.caster()));
        chain.maxConnections = Math.clamp(spell.getMaxConnections(ctx.level(), ctx.caster()), 1, 64);
        return EffectResources.spawn(ctx, chain, 300);
    };
    public static final LocationSpellAdapter HEALING_CIRCLE = (ctx, hit) -> {
        if (hit.getType() == HitResult.Type.MISS) return false;
        var field = new HealingAoe(ctx.world()); field.setOwner(ctx.caster()); field.setCircular();
        field.setRadius((float)radius(ctx, 5)); field.setDuration(duration(ctx, 200)); field.setDamage((float)ctx.power() * .25f);
        field.setPos(center(hit)); return EffectResources.spawn(ctx, field, duration(ctx, 200));
    };
    public static final LocationSpellAdapter BLACK_HOLE = (ctx, hit) -> {
        if (hit.getType() == HitResult.Type.MISS) return false;
        var hole = new BlackHole(ctx.world(), ctx.caster());
        hole.setRadius((float)radius(ctx, 2.0 * ctx.level() + 4 + 3 * (ctx.power() - 1))); hole.setDamage((float)ctx.power() * 2);
        hole.setDuration(duration(ctx, 600)); hole.moveTo(center(hit));
        return EffectResources.spawn(ctx, hole, duration(ctx, 600));
    };
    public static final LocationSpellAdapter TENTACLES = (ctx, hit) -> {
        if (hit.getType() == HitResult.Type.MISS) return false;
        // Bound spawned work, not the level used by native parameters or billing.
        boolean any = false; int rings = (int)Math.min(15, 1L + ctx.level());
        float damage = ((dev.ironsnouveau.mixin.SculkSpellAccess)ctx.spell()).ironsNouveau$damage(ctx.level(), ctx.caster());
        for (int ring = 0; ring < rings; ring++) for (int i = 0; i < 2 + ring * 2; i++) {
            Vec3 pos = center(hit).add(new Vec3(0, 0, radius(ctx, 1.3 * (ring + 1))).yRot((float)(Math.PI * 2 * i / (2 + ring * 2))));
            if (!ctx.world().hasChunkAt(BlockPos.containing(pos))) continue;
            pos = Utils.moveToRelativeGroundLevel(ctx.world(), pos, 8);
            if (ctx.world().getBlockState(BlockPos.containing(pos).below()).isAir()) continue;
            var tentacle = new VoidTentacle(ctx.world(), ctx.caster(), damage); tentacle.moveTo(pos);
            any |= EffectResources.spawn(ctx, tentacle, 301);
        }
        return any;
    };
    public static final LocationSpellAdapter ROOT = (ctx, hit) -> {
        var target = target(ctx, hit); if (target == null || target.getType().is(ModTags.CANT_ROOT) || target.getVehicle() instanceof RootEntity) return false;
        var root = new RootEntity(ctx.world(), ctx.caster());
        int ticks = duration(ctx, (int)(ctx.power() * 20)); root.setDuration(ticks); root.setTarget(target); root.moveTo(target.position());
        root.getAttribute(Attributes.MAX_HEALTH).setBaseValue(40 * ctx.spell().getEntityPowerMultiplier(ctx.caster()));
        root.setHealth(root.getMaxHealth());
        if (!EffectResources.spawn(ctx, root, ticks)) return false;
        target.stopRiding(); target.startRiding(root, true); return true;
    };
    public static final LocationSpellAdapter WISP = (ctx, hit) -> {
        var target = target(ctx, hit); if (target == null) return false;
        var wisp = new WispEntity(ctx.world(), ctx.caster(), (float)ctx.power());
        wisp.setTarget(target); wisp.moveTo(center(hit).add(0, 1, 1)); return EffectResources.spawn(ctx, wisp, 600);
    };
    public static final LocationSpellAdapter FIREFLIES = (ctx, hit) -> {
        var target = target(ctx, hit); if (target == null) return false;
        var swarm = new FireflySwarmProjectile(ctx.world(), ctx.caster(), target, (float)ctx.power() / 3f);
        swarm.setPos(center(hit).add(0, 1, 0)); return EffectResources.spawn(ctx, swarm, 1200);
    };
    public static final SpellAdapter GREATER_HEAL = (ctx, target) -> {
        if (target.getHealth() >= target.getMaxHealth()) return false;
        float amount = target.getMaxHealth();
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new io.redspace.ironsspellbooks.api.events.SpellHealEvent(ctx.caster(), target, amount, ctx.spell().getSchoolType()));
        target.heal(amount); return true;
    };
    public static final SpellAdapter CLEANSE = (ctx, target) -> {
        if (!Utils.shouldHealEntity(ctx.caster(), target)) return false;
        boolean any = false;
        for (var effect : java.util.List.copyOf(target.getActiveEffects()))
            if (effect.getEffect().value().getCategory() == MobEffectCategory.HARMFUL && !effect.getEffect().is(ModTags.CLEANSE_IMMUNE))
                any |= target.removeEffect(effect.getEffect());
        return any;
    };
    public static final SpellAdapter THUNDERSTORM = (ctx, target) -> {
        int ticks = duration(ctx, ((io.redspace.ironsspellbooks.spells.lightning.ThunderstormSpell)ctx.spell()).getDurationTicks(ctx.level(), ctx.caster()));
        int amplifier = ((dev.ironsnouveau.mixin.ThunderstormSpellAccess)ctx.spell()).ironsNouveau$amplifier(ctx.level(), ctx.caster());
        boolean applied = target.addEffect(new MobEffectInstance(MobEffectRegistry.THUNDERSTORM,
                ticks, Math.clamp(amplifier, 0, 255)), ctx.caster());
        if (applied) dev.ironsnouveau.casting.StatusBilling.mark(ctx, target, ticks);
        return applied;
    };
}
