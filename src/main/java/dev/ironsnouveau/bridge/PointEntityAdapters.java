package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.api.SpellAdapter;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.ExtendedEvokerFang;
import io.redspace.ironsspellbooks.entity.spells.StompAoe;
import io.redspace.ironsspellbooks.entity.spells.ice_block.IceBlockProjectile;
import io.redspace.ironsspellbooks.entity.spells.scapegoat.ScapegoatEntity;
import io.redspace.ironsspellbooks.entity.spells.shield.ShieldEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.*;

/** Native delayed entities with explicit owner, placement and bounded lifetime. */
public final class PointEntityAdapters {
    private PointEntityAdapters() {}
    public static final LocationSpellAdapter ICE_BLOCK = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        LivingEntity target = hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity living ? living : null;
        Vec3 pos = WorldSpellAdapters.center(hit);
        int height = 4 + (target == null ? 0 : (int)(target.getBbHeight() * .5));
        for (int i = 0; i < height && ctx.world().getBlockState(BlockPos.containing(pos.add(0, 1, 0))).isAir(); i++) pos = pos.add(0, 1, 0);
        var ice = new IceBlockProjectile(ctx.world(), ctx.caster(), target); ice.moveTo(pos);
        if (!ctx.world().noBlockCollision(ice, ice.getBoundingBox())) ice.noPhysics = true;
        ice.setAirTime(target == null ? 25 : 35); ice.setDamage((float)ctx.power());
        return EffectResources.spawnOnce(ctx, ice, 400);
    };
    public static final LocationSpellAdapter SHIELD = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        Vec3 position = hit.getLocation();
        // Ars Self reports the caster's feet. Native Shield uses an eye-origin, block-clipped
        // three-block ray. Explicit remote/block positions remain under Ars's control.
        if (hit instanceof EntityHitResult entity && entity.getEntity() == ctx.caster())
            position = io.redspace.ironsspellbooks.api.util.RaycastBuilder.begin(ctx.world(), ctx.caster())
                    .range(3).checkForBlocks(true).build().getLocation();
        var shield = new ShieldEntity(ctx.world(), 10 + (float)ctx.power()); shield.setPos(position);
        var rot = Utils.rotationFromDirection(AreaSpellAdapters.direction(ctx, hit)); shield.setRotation(rot.x, rot.y);
        return EffectResources.spawn(ctx, shield, EffectResources.ticks(400 * ctx.duration()));
    };
    public static final LocationSpellAdapter SCAPEGOAT = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        Vec3 pos = Utils.moveToRelativeGroundLevel(ctx.world(), WorldSpellAdapters.center(hit), 4, 24);
        if (!ctx.world().hasChunkAt(BlockPos.containing(pos))) return false;
        var goat = new ScapegoatEntity(ctx.world()); goat.setOwner(ctx.caster()); goat.setTargetPos(BlockPos.containing(pos)); goat.moveTo(pos);
        float yaw = Utils.rotationFromDirection(AreaSpellAdapters.direction(ctx, hit)).y;
        goat.setYRot(yaw); goat.setYBodyRot(yaw); goat.setYHeadRot(yaw);
        goat.getAttribute(Attributes.MAX_HEALTH).setBaseValue(ctx.power()); goat.setHealth(goat.getMaxHealth());
        int ticks = EffectResources.ticks(300 * ctx.duration()); goat.setDurationRemaining(ticks); goat.invulnerableTime = 60;
        if (!EffectResources.spawn(ctx, goat, ticks)) return false;
        goat.poofParticles(20, 1);
        Utils.performTaunt(goat, (float)WorldSpellAdapters.radius(ctx, 12), Utils.tauntPredicate(ctx.caster())); return true;
    };
    public static final LocationSpellAdapter STOMP = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        Vec3 pos = WorldSpellAdapters.center(hit);
        int range = (int)Math.clamp(4 + ctx.level() * (double)ctx.spell().getEntityPowerMultiplier(ctx.caster()), 1, 48);
        var stomp = new StompAoe(ctx.world(), range, Utils.rotationFromDirection(AreaSpellAdapters.direction(ctx, hit)).y);
        stomp.moveTo(pos); stomp.setDamage((float)ctx.power()); stomp.setExplosionRadius(ctx.spell().getEntityPowerMultiplier(ctx.caster())); stomp.setOwner(ctx.caster());
        return EffectResources.spawn(ctx, stomp, 300);
    };
    private static boolean fang(Resolution ctx, Vec3 pos, float yaw, int delay) {
        if (!ctx.world().hasChunkAt(BlockPos.containing(pos))) return false;
        pos = Utils.moveToRelativeGroundLevel(ctx.world(), pos, 8);
        if (ctx.world().getBlockState(BlockPos.containing(pos).below()).isAir()) return false;
        var fang = new ExtendedEvokerFang(ctx.world(), pos.x, pos.y, pos.z, yaw, delay, ctx.caster(), (float)ctx.power());
        return EffectResources.spawnOnce(ctx, fang, delay + 100);
    }
    public static final LocationSpellAdapter FANG_STRIKE = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        Vec3 forward = AreaSpellAdapters.direction(ctx, hit).multiply(1, 0, 1).normalize();
        if (forward.lengthSqr() < 1e-10) forward = new Vec3(0, 0, 1);
        Vec3 center = WorldSpellAdapters.center(hit);
        int count = (int)Math.min(64L, 7L + ctx.level()); boolean any = false;
        float yaw = (float)Math.atan2(forward.z, forward.x);
        for (int i = 0; i < count; i++) any |= fang(ctx, center.add(forward.scale(i)), yaw, i / 3);
        return any;
    };
    public static final LocationSpellAdapter FANG_WARD = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        Vec3 center = WorldSpellAdapters.center(hit); boolean any = false; int spawned = 0;
        int rings = Math.min(12, 2 + (ctx.level() - 1) / 3);
        for (int r = 0; r < rings && spawned < 240; r++) {
            int count = 5 + r * r;
            for (int i = 0; i < count && spawned < 240; i++, spawned++) {
                double angle = Math.PI * 2 * i / count;
                Vec3 offset = new Vec3(Math.cos(angle), 0, Math.sin(angle)).scale(WorldSpellAdapters.radius(ctx, 1.5 * (r + 1)));
                any |= fang(ctx, center.add(offset), (float)angle, r);
            }
        }
        return any;
    };
    public static final LocationSpellAdapter SONIC_BOOM = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        Vec3 start = hit.getLocation(), direction = AreaSpellAdapters.direction(ctx, hit);
        double range = Math.min(128, 15.0 + 5.0 * ctx.level()); Vec3 end = start.add(direction.scale(range));
        // A single travelling shock, not a maintained beam. Preserve the native ability to cross blocks.
        for (var target : ctx.world().getEntities(ctx.caster(), new AABB(start, end).inflate(1)))
            if (Utils.checkEntityIntersecting(target, start, end, .4f).getType() != HitResult.Type.MISS)
                DamageSources.applyDamage(target, (float)ctx.power(), ctx.spell().getDamageSource(ctx.caster()));
        for (int i = 0; i < range; i++) {
            Vec3 pos = start.add(direction.scale(i)); ctx.world().sendParticles(ParticleTypes.SONIC_BOOM, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        }
        return true;
    };
    public static final LocationSpellAdapter DIVINE_SMITE = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        Vec3 pos = WorldSpellAdapters.center(hit);
        float damage = ((dev.ironsnouveau.mixin.DivineSmiteAccess)ctx.spell()).ironsNouveau$damage(ctx.level(), ctx.caster());
        var source = ctx.spell().getDamageSource(ctx.caster());
        for (var target : AreaSpellAdapters.targets(ctx, pos, WorldSpellAdapters.radius(ctx, 2.2)))
            if (DamageSources.applyDamage(target, damage, source))
                net.minecraft.world.item.enchantment.EnchantmentHelper.doPostAttackEffects(ctx.world(), target, source);
        ctx.world().sendParticles(ParticleTypes.ELECTRIC_SPARK, pos.x, pos.y, pos.z, 50, .4, .2, .4, 1); return true;
    };
    public static final SpellAdapter ENDER_CHEST = (ctx, target) -> {
        if (!(target instanceof net.minecraft.server.level.ServerPlayer player)) return false;
        return player.openMenu(new net.minecraft.world.SimpleMenuProvider((id, inventory, actor) ->
                net.minecraft.world.inventory.ChestMenu.threeRows(id, inventory, player.getEnderChestInventory()),
                net.minecraft.network.chat.Component.translatable("container.enderchest"))).isPresent();
    };
}
