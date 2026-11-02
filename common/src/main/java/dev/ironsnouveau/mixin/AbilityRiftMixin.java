package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.ironsnouveau.ability.*;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.undead_spawner.UndeadRiftEntity;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value=UndeadRiftEntity.class,remap=false)
public abstract class AbilityRiftMixin {
    @WrapMethod(method="doSummon")
    private void ironsNouveau$summon(Operation<Void> original){
        var rift=(UndeadRiftEntity)(Object)this;
        if(EffectResources.ability(rift)!=IronAbility.UNDEAD_RIFT){original.call();return;}
        var owner=SummonManager.getOwner(rift);
        if(!(owner instanceof LivingEntity living)||!living.isAlive()||!(rift.level() instanceof ServerLevel world)||!AbilityPayloads.hasSummonRoom(world,living)){
            SummonManager.removeSummon(rift);rift.discard();return;
        }
        EffectResources.prepaidChildren(rift,rift.getPersistentData().getInt("irons_nouveau_summon_duration"),original::call);
    }
    @WrapOperation(method="doSummon",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/capabilities/magic/SummonManager;setDuration(Lnet/minecraft/world/entity/Entity;I)V"))
    private void ironsNouveau$duration(Entity summon,int ticks,Operation<Void> original){
        var rift=(UndeadRiftEntity)(Object)this;
        int duration=EffectResources.ability(rift)==IronAbility.UNDEAD_RIFT?rift.getPersistentData().getInt("irons_nouveau_summon_duration"):ticks;
        original.call(summon,duration);
    }
}
