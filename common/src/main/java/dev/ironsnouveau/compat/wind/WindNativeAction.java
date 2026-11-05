package dev.ironsnouveau.compat.wind;

import dev.arsconflux.api.context.CastContexts;
import dev.arsconflux.api.execution.ExecutionAnchor;
import dev.ironsnouveau.bridge.Resolution;
import dev.ironsnouveau.casting.*;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.projectile.Projectile;

/** Real actor receives motion/effects; the original caster retains power, payment and attribution. */
public final class WindNativeAction {
    private static final ThreadLocal<Resolution> CURRENT=new ThreadLocal<>();
    public static Resolution current(){return CURRENT.get();}
    public static <T> T motionDamage(java.util.function.Supplier<T> action){
        var binding=MotionEffects.current();if(binding==null)return action.get();
        var schools=dev.arsconflux.api.school.SchoolDamage.current();
        return dev.arsconflux.api.damage.SpellPower.during(binding.context(),schools,
                ()->dev.arsconflux.api.damage.NativeSpellDamage.during(binding.context(),schools,action));
    }
    public static Entity powerCaster(Object spell,Entity supplied) {
        var ctx=CURRENT.get();return ctx!=null&&ctx.spell()==spell&&supplied==ctx.executor()?ctx.caster():supplied;
    }
    public static boolean cast(Resolution ctx,LivingEntity actor) {
        var previous=CURRENT.get();CURRENT.set(ctx);
        float yaw=actor.getYRot(),pitch=actor.getXRot();
        var direction=ExecutionAnchor.viewDirection(actor);
        actor.setYRot((float)Math.toDegrees(Math.atan2(-direction.x,direction.z)));
        actor.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
        var data=MagicData.getPlayerMagicData(actor);var saved=data.getAdditionalCastData();
        try {
            return CastContexts.scoped(ctx.context(),()->EffectResources.paidChildren(ctx,200,()->{
                ctx.spell().onCast(ctx.world(),ctx.level(),actor,CastSource.NONE,data);
                WindPlatform.clientCast(actor,ctx.spell(),ctx.level(),data.getAdditionalCastData());
                if(ctx.definition().spellId().getPath().equals("aeropic"))
                    MotionEffects.bind(ctx,actor,20,(float)ctx.power(),Math.max(1,(int)ctx.power()));
                WindPlatform.scaleEffects(actor,ctx);
                return true;
            }));
        } finally {
            data.setAdditionalCastData(saved);actor.setYRot(yaw);actor.setXRot(pitch);
            if(previous==null)CURRENT.remove();else CURRENT.set(previous);
        }
    }
    /** Called while native onCast creates its payloads, before lease/power capture. */
    public static void joined(Entity entity) {
        var ctx=CURRENT.get();if(ctx==null)return;
        if(entity instanceof Projectile projectile)projectile.setOwner(ctx.damageOwner());
        if(entity instanceof dev.ironsnouveau.mixin.WindSlashAccess slash)slash.ironsNouveau$owner(ctx.damageOwner());
        dev.arsconflux.api.damage.NativeSpellDamage.bind(entity,ctx.context());
    }
    private WindNativeAction(){}
}
