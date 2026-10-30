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
    @WrapMethod(method = "applyEffectTick", remap = true)
    private void ironsNouveau$storm(LivingEntity entity, int amplifier, Operation<Void> original) {
        if (!StatusBilling.pulse(entity, () -> { original.call(entity, amplifier); return true; }))
            entity.removeEffect((net.minecraft.world.effect.MobEffect)(Object)this);
    }
    @WrapOperation(method="lambda$applyEffectTick$0",at=@At(value="INVOKE",
            target="Lio/redspace/ironsspellbooks/api/util/Utils;shouldHealEntity(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/entity/Entity;)Z",remap=false))
    private boolean ironsNouveau$stormAllies(net.minecraft.world.entity.Entity host,net.minecraft.world.entity.Entity candidate,Operation<Boolean> original) {
        var owner=host instanceof LivingEntity living?StatusBilling.stormOwner(living):host;
        return owner==candidate||owner!=host&&dev.arsconflux.api.targeting.Allies.areAllies(owner,candidate)
                ||original.call(owner,candidate);
    }
    @ModifyVariable(method="lambda$applyEffectTick$1",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private static LivingEntity ironsNouveau$stormOwner(LivingEntity host){return StatusBilling.stormOwner(host);}
}
