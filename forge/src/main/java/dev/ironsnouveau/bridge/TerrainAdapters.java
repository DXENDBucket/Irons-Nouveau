package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.casting.EffectResources;
import dev.ironsnouveau.mixin.*;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.*;
import io.redspace.ironsspellbooks.entity.spells.ice_spike.IceSpikeEntity;
import io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer;
import io.redspace.ironsspellbooks.util.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;
import net.minecraftforge.common.ForgeHooks;

public final class TerrainAdapters {
    private TerrainAdapters() {}
    public static final LocationSpellAdapter FIRECRACKER = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        // Keep a block-triggered explosion outside the hit surface, as Iron does with its raycast.
        var pos = hit instanceof BlockHitResult block ? hit.getLocation().add(Vec3.atLowerCornerOf(block.getDirection().getNormal()).scale(.25)) : hit.getLocation();
        var rocket = new ExtendedFireworkRocket(ctx.world(), ((FirecrackerAccess)ctx.spell()).ironsNouveau$rocket(),
                ctx.caster(), pos.x, pos.y, pos.z, true, (float)ctx.power());
        if (!EffectResources.spawnOnce(ctx, rocket, 100)) return false;
        // ExtendedFireworkRocket.tick() is empty. Iron's shoot() performs the native explosion,
        // including damage, attribution, the client fireworks event and entity disposal.
        var direction = AreaSpellAdapters.direction(ctx, hit);
        rocket.shoot(direction.x, direction.y, direction.z, 0, 0);
        return true;
    };
    public static final LocationSpellAdapter RAISE_HELL = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        var field = new FireEruptionAoe(ctx.world(), (float)WorldSpellAdapters.radius(ctx, 8));
        field.moveTo(Utils.moveToRelativeGroundLevel(ctx.world(), WorldSpellAdapters.center(hit), 4));
        field.setOwner(ctx.damageOwner()); field.setDamage((float)ctx.power() + WeaponStats.damage(ctx.caster()));
        return EffectResources.spawnOnce(ctx, field, 100);
    };
    public static final LocationSpellAdapter ICE_SPIKES = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        var forward = AreaSpellAdapters.direction(ctx, hit).multiply(1, 0, 1).normalize();
        if (forward.lengthSqr() < 1e-8) forward = new Vec3(0, 0, 1);
        int count = (int)Math.min(64L, 7L + 3L * ctx.level() / 2);
        var center = WorldSpellAdapters.center(hit); boolean any = false;
        for (int i = 0; i < count; i++) {
            var point = center.add(forward.scale(i));
            if (!ctx.world().hasChunkAt(BlockPos.containing(point))) break;
            var ground = Utils.moveToRelativeGroundLevel(ctx.world(), point, 8);
            var below = BlockPos.containing(ground).below();
            if (!ctx.world().getBlockState(below).isFaceSturdy(ctx.world(), below, Direction.UP)) continue;
            var spike = new IceSpikeEntity(ctx.world(), ctx.caster());
            spike.moveTo(ground); spike.setWaitTime(i);
            spike.setSpikeSize(i == count - 1 ? 4.5f : 1 + 2f * i / count);
            spike.setDamage((float)ctx.power() * (i == count - 1 ? 1 : .5f));
            spike.setYRot((float)-Math.toDegrees(Utils.rotationFromDirection(forward).y) - 45);
            any |= EffectResources.spawnOnce(ctx, spike, i + 200);
        }
        return any;
    };
    public static final LocationSpellAdapter FLAMING_STRIKE = (ctx, hit) -> {
        if (!AreaSpellAdapters.loaded(ctx, hit)) return false;
        Vec3 start = WorldSpellAdapters.center(hit), forward = AreaSpellAdapters.direction(ctx, hit);
        double radius = WorldSpellAdapters.radius(ctx, 3.25);
        float damage = (float)ctx.power() + WeaponStats.damage(ctx.caster()) + WeaponStats.fireAspect(ctx.caster());
        var source = ctx.spell().getDamageSource(ctx.damageOwner());
        for (var target : AreaSpellAdapters.targets(ctx, start, radius)) {
            if (target.getBoundingBox().getCenter().subtract(start).dot(forward) < 0) continue;
            if (DamageSources.applyDamage(target, damage, source)) dev.ironsnouveau.platform.CombatAccess.postAttack(ctx.world(), ctx.caster(), target, source);
        }
        var particle = new io.redspace.ironsspellbooks.particle.FlameStrikeParticleOptions((float)forward.x, (float)forward.y, (float)forward.z, false, false, 1);
        var pos = start.add(forward.scale(1.9)).add(0, .5, 0);
        ctx.world().sendParticles(particle, pos.x, pos.y, pos.z, 1, 0, 0, 0, 0);
        return true;
    };
    private static boolean canMine(Resolution ctx, BlockPos pos) {
        if (!ctx.world().hasChunkAt(pos) || !ctx.world().getWorldBorder().isWithinBounds(pos)) return false;
        if (ctx.caster() instanceof ServerPlayer player)
            return !player.isSpectator() && !player.blockActionRestricted(ctx.world(), pos, player.gameMode.getGameModeForPlayer())
                    && ctx.world().mayInteract(player, pos);
        // Block events and native hammer ownership require a real player; never fabricate one for a mob.
        return false;
    }
    public static final LocationSpellAdapter TOUCH_DIG = (ctx, hit) -> {
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK || !canMine(ctx, block.getBlockPos())) return false;
        var pos = block.getBlockPos(); var state = ctx.world().getBlockState(pos);
        if (state.isAir() || !((TouchDigAccess)ctx.spell()).ironsNouveau$canBreak(ctx.world(), pos, ctx.power())) return false;
        var player = (ServerPlayer)ctx.caster();
        if (ForgeHooks.onBlockBreakEvent(ctx.world(), player.gameMode.getGameModeForPlayer(), player, pos) == -1) return false;
        ((TouchDigAccess)ctx.spell()).ironsNouveau$destroy(ctx.world(), pos, ctx.caster());
        ctx.world().sendParticles(ParticleTypes.CRIT, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 15, .2, .2, .2, .1);
        return !ctx.world().getBlockState(pos).equals(state);
    };
    public static final LocationSpellAdapter HAMMER = (ctx, hit) -> {
        if (!(hit instanceof BlockHitResult block) || hit.getType() != HitResult.Type.BLOCK || !canMine(ctx, block.getBlockPos())) return false;
        if (!ctx.world().getBlockState(block.getBlockPos()).is(ModTags.SPECTRAL_HAMMER_MINEABLE)) return false;
        int depth = (int)net.minecraft.util.Mth.clamp(ctx.power(), 0, 16);
        int radius = (int)net.minecraft.util.Mth.clamp(WorldSpellAdapters.radius(ctx, Math.max(ctx.power() * .5, 1)), 1, 8);
        int bound = Math.max(depth, radius);
        for (var pos : BlockPos.betweenClosed(block.getBlockPos().offset(-bound, -bound, -bound), block.getBlockPos().offset(bound, bound, bound)))
            if (!canMine(ctx, pos)) return false;
        var hammer = new SpectralHammer(ctx.world(), ctx.caster(), block, depth, radius);
        var pos = Vec3.atCenterOf(block.getBlockPos());
        if (!block.getDirection().getAxis().isVertical()) pos = pos.add(0, -2, 0).subtract(AreaSpellAdapters.direction(ctx, hit).scale(1.5));
        else if (block.getDirection() == Direction.DOWN) pos = pos.add(0, -3, 0);
        hammer.moveTo(pos);
        return EffectResources.spawnOnce(ctx, hammer, 31);
    };
}
