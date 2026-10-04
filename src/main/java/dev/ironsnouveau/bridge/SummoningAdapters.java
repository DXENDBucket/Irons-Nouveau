package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.casting.EffectResources;
import dev.ironsnouveau.mixin.RaiseDeadAccess;
import io.redspace.ironsspellbooks.api.events.SpellSummonEvent;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.*;
import io.redspace.ironsspellbooks.entity.spells.summoned_weapons.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.common.NeoForge;
import java.util.*;

/** Summoner remains the caster; the Ars hit supplies the formation center and optional enemy target. */
public final class SummoningAdapters {
    private SummoningAdapters() {}
    public static LocationSpellAdapter forSpell(String id) { return (ctx, hit) -> summon(ctx, hit, id); }
    private static boolean summon(Resolution ctx, HitResult hit, String id) {
        if (hit.getType() == HitResult.Type.MISS) return false;
        int nativeCount = switch (ctx.spell()) {
            case io.redspace.ironsspellbooks.spells.evocation.SummonVexSpell spell -> spell.getSummonCount(ctx.level(), ctx.caster());
            case io.redspace.ironsspellbooks.spells.blood.RaiseDeadSpell spell -> spell.getSummonCount(ctx.level(), ctx.caster());
            default -> id.equals("summon_swords") ? 3 : 1;
        };
        int count = Math.clamp(nativeCount, 1, 64);
        boolean any = false;
        for (int i = 0; i < count; i++) {
            Mob mob = create(ctx, id, i); boolean flying = id.equals("summon_vex") || id.equals("summon_swords");
            Vec3 spawn = placement(ctx, mob, WorldSpellAdapters.center(hit), i, count, flying);
            if (spawn == null) { abandon(mob); continue; }
            mob.moveTo(spawn);
            mob.finalizeSpawn(ctx.world(), ctx.world().getCurrentDifficultyAt(mob.blockPosition()), MobSpawnType.MOB_SUMMONED, null);
            configure(ctx, mob, id);
            var event = NeoForge.EVENT_BUS.post(new SpellSummonEvent<>(ctx.caster(), mob, ctx.definition().spellId(), ctx.level()));
            if (mob != event.getCreature()) abandon(mob);
            mob = event.getCreature();
            if (mob == null) continue;
            mob.moveTo(spawn);
            if (!ctx.world().noCollision(mob)) { abandon(mob); continue; }
            int duration = EffectResources.ticks(12000 * ctx.duration());
            if (!EffectResources.spawn(ctx, mob, duration)) { abandon(mob); continue; }
            SummonManager.setOwner(mob, ctx.caster()); SummonManager.setDuration(mob, duration);
            if (hit instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity target
                    && target != ctx.caster() && !ctx.caster().isAlliedTo(target)) mob.setTarget(target);
            any = true;
        }
        return any;
    }
    private static void abandon(Mob mob) { SummonManager.removeSummon(mob); mob.discard(); }
    private static Mob create(Resolution ctx, String id, int index) {
        return switch (id) {
            case "summon_vex" -> new SummonedVex(ctx.world(), ctx.caster());
            case "raise_dead" -> ctx.world().random.nextFloat() < .3f ? new SummonedSkeleton(ctx.world(), ctx.caster(), true) : new SummonedZombie(ctx.world(), ctx.caster(), true);
            case "summon_horse" -> new SummonedHorse(ctx.world(), ctx.caster());
            case "summon_polar_bear" -> new SummonedPolarBear(ctx.world(), ctx.caster());
            case "summon_swords" -> switch (index % 3) {
                case 0 -> new SummonedClaymoreEntity(ctx.world(), ctx.caster());
                case 1 -> new SummonedRapierEntity(ctx.world(), ctx.caster());
                default -> new SummonedSwordEntity(ctx.world(), ctx.caster());
            };
            default -> throw new IllegalArgumentException(id);
        };
    }
    private static void configure(Resolution ctx, Mob mob, String id) {
        if (id.equals("raise_dead")) {
            var access = (RaiseDeadAccess)ctx.spell();
            access.ironsNouveau$equip(mob, access.ironsNouveau$equipment((float)ctx.power(), ctx.world().random));
        } else if (id.equals("summon_horse")) {
            double power = ctx.power() / 100;
            mob.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.22 * power);
            mob.getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(.4 * power);
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(Math.max(1, 15 * power));
            mob.getAttribute(Attributes.SAFE_FALL_DISTANCE).setBaseValue(6 + (int)((.4 * power - .2) * 3));
        } else if (id.equals("summon_polar_bear")) {
            mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(((dev.ironsnouveau.mixin.PolarBearSpellAccess)ctx.spell()).ironsNouveau$health(ctx.level(), ctx.caster()));
            mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(ctx.power());
        } else if (id.equals("summon_swords")) {
            var spell = (io.redspace.ironsspellbooks.spells.ender.SummonSwordsSpell)ctx.spell();
            mob.getAttribute(Attributes.MAX_HEALTH).addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    io.redspace.ironsspellbooks.IronsSpellbooks.id("spell_power_health_bonus"), spell.getHealthBonus(ctx.level(), ctx.caster()),
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            mob.getAttribute(Attributes.ATTACK_DAMAGE).addPermanentModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                    io.redspace.ironsspellbooks.IronsSpellbooks.id("spell_power_damage_bonus"), spell.getDamageBonus(ctx.level(), ctx.caster()),
                    net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        }
        mob.setHealth(mob.getMaxHealth());
    }
    private static Vec3 placement(Resolution ctx, Mob mob, Vec3 center, int index, int count, boolean flying) {
        double base = WorldSpellAdapters.radius(ctx, 1.8);
        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = 2 * Math.PI * index / count + attempt * 2.4;
            double radius = base + attempt / 8.0;
            Vec3 candidate = center.add(Math.cos(angle) * radius, flying ? 1 : 2, Math.sin(angle) * radius);
            for (int down = 0; down < (flying ? 1 : 8); down++) {
                Vec3 pos = candidate.add(0, -down, 0);
                var block = BlockPos.containing(pos);
                if (!ctx.world().hasChunksAt(block.offset(-2, -1, -2), block.offset(2, 3, 2))) continue;
                if (!flying && !ctx.world().getBlockState(block.below()).isFaceSturdy(ctx.world(), block.below(), net.minecraft.core.Direction.UP)) continue;
                mob.setPos(pos);
                if (ctx.world().getWorldBorder().isWithinBounds(mob.getBoundingBox()) && ctx.world().noCollision(mob)
                        && !ctx.world().containsAnyLiquid(mob.getBoundingBox())) return pos;
            }
        }
        return null;
    }
}
