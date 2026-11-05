package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.ironsnouveau.compat.wind.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="net.raptorzizi.wind_spellbooks.entity.spells.wind_blade.WindBladeProjectile",remap=false)
public abstract class WindBladeBehaviorMixin implements WindBladeState {
    @Invoker("damageEntity") public abstract void ironsNouveau$damage(Entity target);
    @Override public com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell confluxTrajectory(){
        var session=ironsNouveau$session();return session!=null&&session.execution() instanceof WindBladeExecution flight?flight.trajectory():null;
    }
    @WrapMethod(method={"tick","m_8119_"},remap=false)
    private void ironsNouveau$motion(Operation<Void> original){
        var entity=(Entity)(Object)this;
        if(ironsNouveau$arsDriven()) {
            WindBladeVisual.tick(entity);return;
        }
        var session=ironsNouveau$session();
        if(session==null){original.call();return;}
        if(!session.activate(()->{original.call();return true;}))entity.discard();
    }
    @WrapOperation(method="damageEntity",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/damage/DamageSources;applyDamage(Lnet/minecraft/world/entity/Entity;FLnet/minecraft/world/damagesource/DamageSource;)Z"))
    private boolean ironsNouveau$hit(Entity target,float amount,DamageSource source,Operation<Boolean> original){
        var session=ironsNouveau$session();
        if(session!=null&&(!session.permitted()||dev.arsconflux.api.targeting.Allies.areAllies(session.caster(),target)))return false;
        boolean success=original.call(target,amount,source);
        if(success&&session!=null)session.impact(new EntityHitResult(target,((Entity)(Object)this).position()));
        return success;
    }
    @Inject(method={"onHitBlock","m_8060_"},at=@At("TAIL"),remap=false)
    private void ironsNouveau$blockHit(BlockHitResult hit,CallbackInfo ci){
        var session=ironsNouveau$session();if(session!=null)session.impact(hit);
    }
}
