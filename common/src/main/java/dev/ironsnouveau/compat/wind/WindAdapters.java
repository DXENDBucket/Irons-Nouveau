package dev.ironsnouveau.compat.wind;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.bridge.*;
import dev.ironsnouveau.casting.EffectResources;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.raptorzizi.wind_spellbooks.entity.spells.tornado.TornadoEntity;
import net.raptorzizi.wind_spellbooks.entity.spells.almighty_push.AlmightyPushEntity;
import net.raptorzizi.wind_spellbooks.spells.wind.TornadoSpell;
import net.raptorzizi.wind_spellbooks.spells.wind.AlmightyPushSpell;

/** Preserve native entities and native action callbacks; Ars supplies the selected execution point. */
public final class WindAdapters {
    public static LocationSpellAdapter forSpell(String spell) {
        return switch(spell) {
            case "tornado" -> WindAdapters::tornado;
            case "almighty_push" -> WindAdapters::push;
            default -> WindAdapters::action;
        };
    }
    private static boolean loaded(Resolution ctx,HitResult hit) {
        return hit.getType()!=HitResult.Type.MISS&&ctx.world().hasChunkAt(BlockPos.containing(hit.getLocation()));
    }
    private static boolean tornado(Resolution ctx,HitResult hit) {
        if(!loaded(ctx,hit))return false;
        var spell=(TornadoSpell)ctx.spell();
        var tornado=new TornadoEntity(ctx.world(),ctx.damageOwner());
        tornado.setRadius((float)WorldSpellAdapters.radius(ctx,spell.getRadius(ctx.level(),ctx.caster())));
        tornado.setDamage(((dev.ironsnouveau.mixin.WindTornadoAccess)spell).ironsNouveau$damage(ctx.level(),ctx.caster()));
        int duration=EffectResources.ticks(spell.getDuration(ctx.level())*ctx.duration());
        tornado.setDuration(duration);
        tornado.setPos(hit.getLocation());
        return EffectResources.spawnOnce(ctx,tornado,duration+20);
    }
    private static boolean push(Resolution ctx,HitResult hit) {
        if(!loaded(ctx,hit))return false;
        var spell=(AlmightyPushSpell)ctx.spell();
        var push=new AlmightyPushEntity(ctx.world(),ctx.damageOwner());
        float radius=(float)WorldSpellAdapters.radius(ctx,spell.getRange(ctx.level(),ctx.caster()));
        push.setRadius(radius);push.strength=spell.getStrength(ctx.level(),ctx.caster());push.amplifier=ctx.level()-1;
        int duration=EffectResources.ticks(spell.getDuration(ctx.level())*ctx.duration());push.setLifetime(duration);
        var center=hit instanceof EntityHitResult e?e.getEntity().getBoundingBox().getCenter():hit.getLocation();
        push.setPos(center.x,center.y-radius,center.z);
        if(!EffectResources.spawnOnce(ctx,push,duration+20))return false;
        WindPlatform.pushSound(ctx.world(),center);return true;
    }
    private static boolean action(Resolution ctx,HitResult hit) {
        if(!loaded(ctx,hit)||!(hit instanceof EntityHitResult e)||!(e.getEntity() instanceof LivingEntity actor)
                ||!actor.isAlive()||actor.isSpectator()||actor.isPassenger()||actor.level()!=ctx.world()
                ||!Targeting.alliedExecutor(ctx.caster(),actor))return false;
        boolean applied=false;
        for(var selected:Targeting.select(ctx.world(),ctx.caster(),hit,AugmentScaling.radius(ctx.stats().getAoeMultiplier()),false))
            if(Targeting.alliedExecutor(ctx.caster(),selected)&&!selected.isPassenger())
                applied|=WindNativeAction.cast(ctx.withTarget(new EntityHitResult(selected)).withExecutor(selected),selected);
        return applied;
    }
    private WindAdapters(){}
}
