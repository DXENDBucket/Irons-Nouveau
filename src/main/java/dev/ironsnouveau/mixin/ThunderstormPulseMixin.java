package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.StatusBilling;
import io.redspace.ironsspellbooks.effect.ThunderstormEffect;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(value = ThunderstormEffect.class, remap = false)
public abstract class ThunderstormPulseMixin {
    @WrapMethod(method = "applyEffectTick")
    private boolean ironsNouveau$storm(LivingEntity entity, int amplifier, Operation<Boolean> original) {
        return StatusBilling.pulse(entity, () -> original.call(entity, amplifier));
    }
    @WrapOperation(method="lambda$applyEffectTick$0",at=@At(value="INVOKE",
            target="Lio/redspace/ironsspellbooks/api/util/Utils;shouldHealEntity(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean ironsNouveau$stormAllies(LivingEntity host,LivingEntity candidate,Operation<Boolean> original) {
        var owner=StatusBilling.stormOwner(host);
        return owner==candidate||owner!=host&&dev.arsconflux.api.targeting.Allies.areAllies(owner,candidate)
                ||original.call(owner,candidate);
    }
    @ModifyVariable(method="lambda$applyEffectTick$1",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static LivingEntity ironsNouveau$stormOwner(LivingEntity host){return StatusBilling.stormOwner(host);}
}
