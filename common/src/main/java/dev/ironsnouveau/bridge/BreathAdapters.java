package dev.ironsnouveau.bridge;
import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.casting.*;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.poison_breath.PoisonBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.dragon_breath.DragonBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.cone_of_cold.ConeOfColdProjectile;

public final class BreathAdapters {
    private BreathAdapters() {}
    private static float unsupported(String id) { throw new IllegalArgumentException(id); }
    public static LocationSpellAdapter of(String id) { return (ctx, hit) -> {
        var pose = BreathPose.forBreath(ctx, hit);
        AbstractConeProjectile cone = switch (id) {
            case "fire_breath" -> new FireBreathProjectile(ctx.world(), ctx.caster());
            case "poison_breath" -> new PoisonBreathProjectile(ctx.world(), ctx.caster());
            case "dragon_breath" -> new DragonBreathProjectile(ctx.world(), ctx.caster());
            case "cone_of_cold" -> new ConeOfColdProjectile(ctx.world(), ctx.caster());
            case "electrocute" -> new io.redspace.ironsspellbooks.entity.spells.electrocute.ElectrocuteProjectile(ctx.world(), ctx.caster());
            default -> throw new IllegalArgumentException(id);
        };
        float damage = ctx.spell() instanceof io.redspace.ironsspellbooks.spells.fire.FireBreathSpell spell ? spell.getDamage(ctx.level(), ctx.caster()) :
                ctx.spell() instanceof io.redspace.ironsspellbooks.spells.nature.PoisonBreathSpell spell ? spell.getDamage(ctx.level(), ctx.caster()) :
                ctx.spell() instanceof io.redspace.ironsspellbooks.spells.ender.DragonBreathSpell spell ? spell.getDamage(ctx.level(), ctx.caster()) :
                ctx.spell() instanceof io.redspace.ironsspellbooks.spells.ice.ConeOfColdSpell spell ? spell.getDamage(ctx.level(), ctx.caster()) :
                ctx.spell() instanceof io.redspace.ironsspellbooks.spells.lightning.ElectrocuteSpell spell ? spell.getDamage(ctx.level(), ctx.caster()) : unsupported(id);
        int duration = EffectResources.ticks(ctx.spell().getCastTime(ctx.level()) * ctx.duration());
        float scale = (float)net.minecraft.util.Mth.clamp(1 + Math.max(0, ctx.stats().getAoeMultiplier()) * .25, 1, 3);
        var plan = new CastPlan(ctx.definition().glyphId(), ctx.definition().spellId(), ctx.level(), damage, new CastModifiers(0, 1), duration);
        var frame = ctx.context().withTarget(hit).withExecutor(pose.anchor());
        var account = TriggerMana.of(frame);
        return CastSessions.start(new CastSession(ctx.world(), frame, plan, pose, new BreathExecution(cone, pose, scale), ignored -> {})
                .billing(account, true));
    }; }
}
