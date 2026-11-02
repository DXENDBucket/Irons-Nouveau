package dev.ironsnouveau.ability;

import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.damage.SpellPower;
import dev.arsconflux.api.school.SchoolDamage;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.mixin.NativeProjectileHitAccess;
import io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.*;

public final class DaggerFlight {
    public static boolean replace(EntityProjectileSpell carrier,CarrierProfile profile){
        if(!(carrier.level() instanceof ServerLevel world))return false;
        var resolver=ArsSpellAccess.resolver(carrier);if(resolver==null)return false;
        var frame=CastContext.of(resolver.spellContext,null,null);if(frame.caster()==null)return true;
        var quote=AbilityCatalog.DAGGER.initialCharge(resolver.spellContext,resolver.getCastStats().getAugments());
        var selected=frame.withAccount(quote.account()).withIncomingDirection(dev.arsconflux.api.context.TriggerGeometry.direction(carrier));
        var dagger=new FieryDaggerEntity(world);dagger.setOwner(frame.damageOwner());dagger.setPos(carrier.position());
        dagger.setDamage(AbilityPayloads.damage(frame.caster(),resolver.getCastStats().getAmpMultiplier(),7.5f));
        double speedScale=profile.arsDriven()?1:carrier.getDeltaMovement().length()/.75;
        dagger.getPersistentData().putDouble("irons_nouveau_speed_scale",speedScale);
        dagger.setDeltaMovement(carrier.getDeltaMovement().normalize().scale(dagger.getSpeed()));dagger.delay=20;
        if(frame.caster() instanceof Mob mob&&mob.getTarget()!=null)dagger.setTarget(mob.getTarget());
        else {
            var aimed=carrier.position().add(carrier.getDeltaMovement().normalize().scale(32));
            var end=world.clip(new net.minecraft.world.level.ClipContext(carrier.position(),aimed,
                    net.minecraft.world.level.ClipContext.Block.COLLIDER,net.minecraft.world.level.ClipContext.Fluid.NONE,frame.caster())).getLocation();
            var hit=net.minecraft.world.entity.projectile.ProjectileUtil.getEntityHitResult(world,frame.caster(),carrier.position(),end,
                    new AABB(carrier.position(),end).inflate(1),e->e.isAlive()&&e instanceof net.minecraft.world.entity.LivingEntity
                            &&!dev.arsconflux.api.targeting.Allies.areAllies(frame.caster(),e),.3f);
            if(hit!=null)dagger.setTarget(hit.getEntity());
        }
        var saved=resolver.clone();var trajectory=profile.arsDriven()?new ArsTrajectory(dagger,carrier):null;
        var driver=new ExecutionDriver<FieryDaggerEntity>(){
            @Override public boolean start(ExecutionSession<FieryDaggerEntity> session){
                ((DaggerState)dagger).ironsNouveau$abilitySession(session);
                SpellPower.bind(dagger,new SpellPower.Profile(selected,SchoolDamage.current()));
                if(trajectory!=null){
                    ((DaggerState)dagger).ironsNouveau$trajectory(carrier);
                    ((NativeCastCarrier)dagger).ironsNouveau$arsDriven(true);
                    return trajectory.start(world,()->{},hit->((NativeProjectileHitAccess)dagger).ironsNouveau$hit(hit));
                }
                boolean added=world.addFreshEntity(dagger);if(added)session.trackProjectile(dagger);return added;
            }
            @Override public boolean tick(ExecutionSession<FieryDaggerEntity> session){return trajectory==null?dagger.isRemoved():trajectory.tick(session.world());}
            @Override public net.minecraft.world.phys.Vec3 incomingDirection(){return trajectory==null?dagger.getDeltaMovement():trajectory.incomingDirection();}
            @Override public void close(ExecutionSession<FieryDaggerEntity> session,ExecutionSession.EndReason reason){
                ((DaggerState)dagger).ironsNouveau$abilitySession(null);((DaggerState)dagger).ironsNouveau$trajectory(null);
                if(trajectory!=null)trajectory.close();else {dagger.discard();carrier.discard();}
            }
        };
        var session=new ExecutionSession<>(world,selected,dagger,Math.max(60,Math.min(1200,carrier.getExpirationTime()+40)),driver,
                hit->{var child=saved.clone();CastContexts.bind(child.spellContext,CastContexts.current().withAccount(frame.account()));child.onResolveEffect((ServerLevel)dagger.level(),hit);},
                IronAbility.FIERY_DAGGER::enabled,ExecutionScope.DIRECT).billing(quote::trigger,profile.repeated());
        ExecutionSessions.start(session);return true;
    }
    public static void impact(FieryDaggerEntity dagger,HitResult hit,Runnable nativeImpact){
        var session=((DaggerState)dagger).ironsNouveau$abilitySession();
        if(session==null){nativeImpact.run();return;}
        if(!session.activate(()->{
            if(ImpactTriggers.resolve(hit,ImpactTriggers.Policy.ENTITY_DAMAGE,nativeImpact))session.impact(hit);return true;
        }))session.finish(ExecutionSession.EndReason.DENIED);
    }
    private DaggerFlight(){}
}
