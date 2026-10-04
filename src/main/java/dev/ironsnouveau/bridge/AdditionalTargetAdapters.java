package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.api.SpellAdapter;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.api.entity.IMagicEntity;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.capabilities.magic.RecastResult;
import io.redspace.ironsspellbooks.effect.EchoingStrikesData;
import io.redspace.ironsspellbooks.effect.MagicMobEffect;
import io.redspace.ironsspellbooks.entity.mobs.AntiMagicSusceptible;
import io.redspace.ironsspellbooks.entity.spells.blood_needle.BloodNeedle;
import io.redspace.ironsspellbooks.entity.spells.devour_jaw.DevourJaw;
import io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.spells.blood.DevourSpell;
import io.redspace.ironsspellbooks.spells.blood.SacrificeSpell;
import io.redspace.ironsspellbooks.spells.ender.EchoingStrikesSpell;
import io.redspace.ironsspellbooks.spells.ice.IceTombSpell;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.Sheep;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/** Native status formulas use the original caster; Ars only selects the recipient. */
public final class AdditionalTargetAdapters {
    private AdditionalTargetAdapters() {}
    private static SpellAdapter status(Holder<MobEffect> effect, ToDoubleFunction<Resolution> ticks, ToIntFunction<Resolution> amp) {
        return (ctx, target) -> target.addEffect(new MobEffectInstance(effect,
                EffectResources.ticks(ticks.applyAsDouble(ctx) * ctx.duration()),
                Math.clamp(amp.applyAsInt(ctx), 0, 255), false, false, true), ctx.caster());
    }
    public static final SpellAdapter HEARTSTOP = status(MobEffectRegistry.HEARTSTOP, Resolution::power, c -> 0);
    public static final SpellAdapter SHROUD = status(MobEffectRegistry.ABYSSAL_SHROUD, c -> (int)c.power() * 20.0, c -> 0);
    public static final SpellAdapter SIGHT = status(MobEffectRegistry.PLANAR_SIGHT, c -> c.power() * 20, c -> c.level() - 1);
    public static final SpellAdapter INVISIBILITY = status(MobEffectRegistry.TRUE_INVISIBILITY, c -> c.power() * 20, c -> 0);
    public static final SpellAdapter WINGS = status(MobEffectRegistry.ANGEL_WINGS, c -> (int)c.power() * 20.0, c -> 0);
    public static final SpellAdapter FROSTBITE = status(MobEffectRegistry.FROSTBITTEN_STRIKES, c -> c.power() * 20, c -> (int)Math.min(255L, (long)c.level() + 4));
    public static final SpellAdapter CHARGE = status(MobEffectRegistry.CHARGED, c -> 600 * c.spell().getEntityPowerMultiplier(c.caster()), c -> c.level() - 1);
    public static final SpellAdapter GLUTTONY = status(MobEffectRegistry.GLUTTONY, c -> c.power() * 20, c -> c.level() - 1);
    public static final SpellAdapter SPIDER = status(MobEffectRegistry.SPIDER_ASPECT, c -> c.power() * 20, c -> c.level() - 1);
    private static final SpellAdapter ECHO_STATUS = status(MobEffectRegistry.ECHOING_STRIKES, c -> 2400, c -> 14);
    public static final SpellAdapter ECHO = (ctx, target) -> {
        boolean applied = ECHO_STATUS.apply(ctx, target);
        if (applied) EchoingStrikesData.get(target).setHitCount(((EchoingStrikesSpell)ctx.spell()).getHitCount(ctx.level(), ctx.caster()));
        return applied;
    };
    public static final SpellAdapter WOLOLO = (ctx, target) -> {
        if (!(target instanceof Sheep sheep)) return false;
        sheep.setColor(DyeColor.values()[ctx.world().random.nextInt(DyeColor.values().length)]); return true;
    };
    public static final SpellAdapter SACRIFICE = (ctx, target) -> {
        if (!(target instanceof IMagicSummon summon) || summon.getSummoner() != ctx.caster()) return false;
        var spell = (SacrificeSpell)ctx.spell();
        SacrificeSpell.doSacrificeExplosion(ctx.world(), spell.getDamageSource(target, ctx.caster()),
                spell.getDamage(ctx.level(), ctx.caster()) + target.getHealth() * .5f,
                (float)WorldSpellAdapters.radius(ctx, spell.getRadius(target)), target.getBoundingBox().getCenter());
        io.redspace.ironsspellbooks.capabilities.magic.SummonManager.removeSummon(target);
        target.remove(net.minecraft.world.entity.Entity.RemovalReason.KILLED); return true;
    };
    public static final SpellAdapter DEVOUR = (ctx, target) -> {
        var spell = (DevourSpell)ctx.spell();
        var jaw = new DevourJaw(ctx.world(), ctx.caster(), target);
        jaw.moveTo(target.position()); jaw.setYRot(ctx.caster().getYRot());
        jaw.setDamage(spell.getDamage(ctx.level(), ctx.caster()));
        jaw.vigorLevel = spell.getHpBonus(ctx.level(), ctx.caster()) / 2 - 1;
        return EffectResources.spawnOnce(ctx, jaw, 100);
    };
    public static final SpellAdapter ACUPUNCTURE = (ctx, target) -> {
        int count = (int)Math.clamp((4.0 + ctx.level()) * ctx.power(), 1, 64);
        Vec3 center = target.position().add(0, target.getEyeHeight() * .5, 0);
        boolean any = false;
        for (int i = 0; i < count; i++) {
            var offset = new Vec3(0, ctx.world().random.nextDouble(), .55).normalize()
                    .scale(target.getBbWidth() + 2.75).yRot((float)(Math.PI * 2 * i / count));
            var needle = new BloodNeedle(ctx.world(), ctx.caster()); needle.moveTo(center.add(offset));
            needle.shoot(offset.normalize().scale(-.35)); needle.setDamage((float)(1 + ctx.power())); needle.setScale(.4f);
            any |= EffectResources.spawnOnce(ctx, needle, 200);
        }
        return any;
    };
    public static final SpellAdapter ICE_TOMB = (ctx, target) -> {
        if (target.getVehicle() instanceof IceTombEntity) return false;
        var spell = (IceTombSpell)ctx.spell(); var tomb = new IceTombEntity(ctx.world(), ctx.caster());
        int duration = EffectResources.ticks(spell.getDuration(ctx.level(), ctx.caster()) * ctx.duration());
        tomb.moveTo(target.position()); tomb.setDeltaMovement(target.getDeltaMovement());
        tomb.setHealing(spell.getHealing(ctx.level(), ctx.caster())); tomb.setLifetime(duration);
        if (!EffectResources.spawn(ctx, tomb, duration)) return false;
        if (!target.startRiding(tomb, true)) { tomb.discard(); return false; }
        return true;
    };
    public static final LocationSpellAdapter COUNTERSPELL = (ctx, hit) -> {
        if (!(hit instanceof EntityHitResult entityHit)) return false;
        var target = entityHit.getEntity();
        if (!Utils.validAntiMagicTarget(target) || NeoForge.EVENT_BUS.post(new CounterSpellEvent(ctx.caster(), target)).isCanceled()) return false;
        if (target instanceof AntiMagicSusceptible susceptible) {
            if (!(target instanceof IMagicSummon summon) || summon.getSummoner() != ctx.caster()
                    || target instanceof Mob mob && mob.getTarget() == null)
                susceptible.onAntiMagic(MagicData.getPlayerMagicData(ctx.caster()));
        } else if (target instanceof ServerPlayer player) {
            Utils.serverSideCancelCast(player, true);
            MagicData.getPlayerMagicData(player).getPlayerRecasts().removeAll(RecastResult.COUNTERSPELL);
        } else if (target instanceof IMagicEntity caster) caster.cancelCast();
        if (target instanceof net.minecraft.world.entity.LivingEntity living)
            for (var effect : java.util.List.copyOf(living.getActiveEffectsMap().keySet()))
                if (effect.value() instanceof MagicMobEffect) living.removeEffect(effect);
        return true;
    };
}
