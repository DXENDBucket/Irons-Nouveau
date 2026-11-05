package dev.ironsnouveau.compat.wind;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.projectile.ArsTrajectory;
import dev.ironsnouveau.casting.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.*;
import net.raptorzizi.wind_spellbooks.entity.spells.wind_blade.WindBladeProjectile;
public final class WindBladeExecution implements CastExecution {
    private final WindBladeProjectile blade;
    private ArsTrajectory motion;
    private EntityProjectileSpell trajectory;
    public WindBladeExecution(ServerLevel world,LivingEntity caster){blade=new WindBladeProjectile(world,caster);}
    @Override public CastExecution withTrajectory(EntityProjectileSpell carrier){trajectory=carrier;motion=new ArsTrajectory(blade,carrier);return this;}
    public EntityProjectileSpell trajectory(){return trajectory;}
    @Override public boolean start(CastSession session){
        blade.setOwner(session.damageOwner());blade.setDamage(session.plan().nativePower()*.5f);
        dev.arsconflux.api.damage.NativeSpellDamage.bind(blade,session.context());
        var state=(WindBladeState)blade;state.ironsNouveau$session(session);
        if(motion!=null)return motion.start(session.world(),()->state.ironsNouveau$arsDriven(true),hit->impact(session,hit));
        blade.setPos(session.aim().origin());
        blade.shoot(session.aim().direction().scale(session.plan().modifiers().speedMultiplier()));
        var direction=blade.getDeltaMovement();
        blade.setYRot((float)-Math.toDegrees(Math.atan2(direction.x,direction.z)));
        blade.setXRot((float)-Math.toDegrees(Math.atan2(direction.y,direction.horizontalDistance())));
        blade.yRotO=blade.getYRot();blade.xRotO=blade.getXRot();
        return session.world().addFreshEntity(blade);
    }
    private void impact(CastSession session,HitResult hit){
        session.activate(()->{
            if(hit instanceof EntityHitResult e)return dev.arsconflux.api.projectile.ImpactTriggers.resolve(hit,
                    dev.arsconflux.api.projectile.ImpactTriggers.Policy.ENTITY_DAMAGE,()->((WindBladeState)blade).ironsNouveau$damage(e.getEntity()));
            else if(hit.getType()==HitResult.Type.BLOCK)session.impact(hit);
            return true;
        });
    }
    @Override public boolean tick(CastSession session){
        if(motion==null)return blade.isRemoved();
        boolean done=motion.tick(session.world());
        var velocity=blade.getDeltaMovement();
        blade.setYRot((float)-Math.toDegrees(Math.atan2(velocity.x,velocity.z)));
        blade.setXRot((float)-Math.toDegrees(Math.atan2(velocity.y,velocity.horizontalDistance())));
        return done;
    }
    @Override public Vec3 incomingDirection(){return motion==null?blade.getDeltaMovement():motion.incomingDirection();}
    @Override public void close(CastSession session,CastSession.EndReason reason){
        ((WindBladeState)blade).ironsNouveau$session(null);
        if(motion!=null)motion.close();else blade.discard();
    }
}
