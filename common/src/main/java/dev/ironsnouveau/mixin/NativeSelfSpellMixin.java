package dev.ironsnouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import com.llamalad7.mixinextras.sugar.Local;
import dev.ironsnouveau.casting.NativeSelfCasts;
import dev.ironsnouveau.config.SpellLevelConfig;
import io.redspace.ironsspellbooks.api.magic.*;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import io.redspace.ironsspellbooks.capabilities.magic.PlayerCooldowns;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(value=AbstractSpell.class,remap=false)
public abstract class NativeSelfSpellMixin {
    @Inject(method="getEffectiveCastTime",at=@At("RETURN"),cancellable=true)
    private void ironsNouveau$prepared(int level,LivingEntity caster,CallbackInfoReturnable<Integer> cir) {
        var spell=(AbstractSpell)(Object)this;
        if(spell.getCastType()==CastType.LONG&&NativeSelfCasts.starting(spell,caster))cir.setReturnValue(0);
    }
    @WrapMethod(method="castSpell")
    private void ironsNouveau$frame(Level world,int level,ServerPlayer player,CastSource source,boolean cooldown,Operation<Void> original) {
        NativeSelfCasts.around((AbstractSpell)(Object)this,player,()->original.call(world,level,player,source,cooldown));
    }
    @WrapOperation(method="castSpell",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"))
    private void ironsNouveau$pay(AbstractSpell spell,Level world,int level,LivingEntity caster,CastSource source,MagicData data,Operation<Void> original,@Local SpellOnCastEvent event) {
        NativeSelfCasts.effect(spell,caster,event,()->original.call(spell,world,level,caster,source,data));
    }
    @Inject(method="castSpell",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V",shift=At.Shift.AFTER),cancellable=true)
    private void ironsNouveau$unpaid(Level world,int level,ServerPlayer player,CastSource source,boolean cooldown,CallbackInfo ci) {
        if(NativeSelfCasts.failed((AbstractSpell)(Object)this,player))ci.cancel();
    }
    @WrapOperation(method="canBeCastedBy",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/capabilities/magic/PlayerCooldowns;isOnCooldown(Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;)Z"))
    private boolean ironsNouveau$optionalCooldown(PlayerCooldowns cooldowns,AbstractSpell spell,Operation<Boolean> original) {
        var self=(AbstractSpell)(Object)this;
        // Scope is only present during the opted-in native initiation.
        return !SpellLevelConfig.cooldownsEnabled()&&NativeSelfCasts.isStarting(self)?false:original.call(cooldowns,spell);
    }
}
