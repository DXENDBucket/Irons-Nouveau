package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.execution.ExecutionSession;
import dev.ironsnouveau.ability.*;
import dev.ironsnouveau.casting.*;
import io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity;
import io.redspace.ironsspellbooks.entity.spells.magma_ball.FireField;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=FieryDaggerEntity.class,remap=false)
public abstract class AbilityDaggerMixin implements DaggerState {
    @Unique private ExecutionSession<FieryDaggerEntity> ironsNouveau$abilitySession;
    @Unique private EntityProjectileSpell ironsNouveau$trajectory;
    public ExecutionSession<FieryDaggerEntity> ironsNouveau$abilitySession(){return ironsNouveau$abilitySession;}
    public void ironsNouveau$abilitySession(ExecutionSession<FieryDaggerEntity> s){ironsNouveau$abilitySession=s;}
    public EntityProjectileSpell ironsNouveau$trajectory(){return ironsNouveau$trajectory;}
    public void ironsNouveau$trajectory(EntityProjectileSpell s){ironsNouveau$trajectory=s;}
    @Inject(method="tick",remap=true,at=@At("HEAD"),cancellable=true)
    private void ironsNouveau$motion(CallbackInfo ci){
        var dagger=(FieryDaggerEntity)(Object)this;
        if(!((NativeCastCarrier)dagger).ironsNouveau$arsDriven())return;
        if(dagger.level().isClientSide){dagger.setPos(dagger.position().add(dagger.getDeltaMovement()));dagger.trailParticles();}
        ci.cancel();
    }
    @WrapMethod(method="getSpeed")
    private float ironsNouveau$speed(Operation<Float> original){
        var data=((FieryDaggerEntity)(Object)this).getPersistentData();
        return original.call()*(float)(data.contains("irons_nouveau_speed_scale")?data.getDouble("irons_nouveau_speed_scale"):1);
    }
    // The Boss dagger calls Entity.hurt directly, bypassing Iron's SpellDamageEvent bridge.
    @WrapOperation(method="onHitEntity",remap=true,at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",remap=true))
    private boolean ironsNouveau$damage(Entity target,net.minecraft.world.damagesource.DamageSource source,float amount,Operation<Boolean> original){
        var dagger=(FieryDaggerEntity)(Object)this;
        if(ironsNouveau$abilitySession==null&&!EffectResources.managed(dagger))return original.call(target,source,amount);
        var profile=dev.arsconflux.api.damage.SpellPower.profile(dagger);
        if(profile==null)return original.call(target,source,amount);
        if(profile.damageOwner()!=null&&dev.arsconflux.api.targeting.Allies.areAllies(profile.damageOwner(),target))return false;
        var frame=dev.arsconflux.api.context.CastContext.detached(profile.caster(),new EntityHitResult(target),null)
                .withExecutor(profile.executor()).withDamageOwner(profile.damageOwner());
        return EffectResources.pulse(dagger,()->dev.arsconflux.api.damage.NativeSpellDamage.hurt(frame,profile.schools(),target,source,amount,
                (next,value)->original.call(target,next,value)));
    }
    @WrapMethod(method="onHit",remap=true)
    private void ironsNouveau$impact(HitResult hit,Operation<Void> original){DaggerFlight.impact((FieryDaggerEntity)(Object)this,hit,()->original.call(hit));}
    @WrapMethod(method="createDaggerZone")
    private void ironsNouveau$children(Vec3 center,Operation<Void> original){
        var dagger=(FieryDaggerEntity)(Object)this;
        if(!EffectResources.managed(dagger)){original.call(center);return;}
        EffectResources.prepaidChildren(dagger,600,()->original.call(center));
    }
    // The enclosing helper is Iron-owned; this inherited Projectile method still needs Forge's SRG mapping.
    @WrapOperation(method="createFireField",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/entity/spells/magma_ball/FireField;setOwner(Lnet/minecraft/world/entity/Entity;)V",remap=true))
    private void ironsNouveau$fieldOwner(FireField field,Entity nativeOwner,Operation<Void> original){
        var dagger=(FieryDaggerEntity)(Object)this;
        original.call(field,EffectResources.managed(dagger)?dagger.getOwner():nativeOwner);
    }
    @WrapOperation(method="createFireField",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/entity/spells/magma_ball/FireField;setDuration(I)V"))
    private void ironsNouveau$fieldDuration(FireField field,int duration,Operation<Void> original){
        var dagger=(FieryDaggerEntity)(Object)this;
        double scale=dagger.getPersistentData().getDouble("irons_nouveau_duration_scale");
        original.call(field,EffectResources.managed(dagger)&&scale>0?EffectResources.ticks(duration*scale):duration);
    }
}
