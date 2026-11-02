package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.ironsnouveau.ability.IronAbility;
import dev.ironsnouveau.casting.EffectResources;
import dev.arsconflux.api.targeting.Allies;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.fire_orb.OminousFireOrbEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=OminousFireOrbEntity.class,remap=false)
public abstract class AbilityOrbMixin {
    @WrapMethod(method="doExplosion")
    private void ironsNouveau$explode(Operation<Void> original){
        var orb=(OminousFireOrbEntity)(Object)this;
        if(EffectResources.ability(orb)!=IronAbility.OMINOUS_FIRE_ORB){original.call();return;}
        EffectResources.executeOnce(orb,original::call);
    }
    @WrapOperation(method="doExplosion",at=@At(value="INVOKE",target="Lnet/minecraft/world/entity/Entity;hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z",remap=true))
    private boolean ironsNouveau$protect(Entity target,DamageSource source,float damage,Operation<Boolean> original){
        var orb=(OminousFireOrbEntity)(Object)this;
        if(EffectResources.ability(orb)==IronAbility.OMINOUS_FIRE_ORB&&orb.getOwner()!=null&&Allies.areAllies(orb.getOwner(),target))return false;
        return original.call(target,source,damage);
    }
    @WrapMethod(method="isAlliedTo",remap=true)
    private boolean ironsNouveau$allied(Entity target,Operation<Boolean> original){
        var orb=(OminousFireOrbEntity)(Object)this;
        if(EffectResources.ability(orb)!=IronAbility.OMINOUS_FIRE_ORB)return original.call(target);
        return orb.getOwner()!=null&&Allies.areAllies(orb.getOwner(),target);
    }
}
