package dev.ironsnouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.NativeSelfCasts;
import dev.ironsnouveau.config.SpellLevelConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;

@Mixin(value=MagicManager.class,remap=false)
public abstract class NativeSelfManagerMixin {
    @WrapMethod(method="addCooldown")
    private void ironsNouveau$cooldown(ServerPlayer player,AbstractSpell spell,CastSource source,Operation<Void> original) {
        if(!NativeSelfCasts.managedCooldown(spell,player)){original.call(player,spell,source);return;}
        if(SpellLevelConfig.cooldownsEnabled())original.call(player,spell,CastSource.SPELLBOOK);
    }
    @WrapOperation(method="lambda$tick$0",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onServerCastComplete(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;Z)V"))
    private void ironsNouveau$cancelUnpaid(AbstractSpell spell,Level world,int level,LivingEntity entity,MagicData data,boolean cancelled,Operation<Void> original) {
        original.call(spell,world,level,entity,data,cancelled||NativeSelfCasts.failed(spell,entity));
    }
    @WrapOperation(method="lambda$tick$0",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onServerCastTick(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"))
    private void ironsNouveau$skipUnpaidTick(AbstractSpell spell,Level world,int level,LivingEntity entity,MagicData data,Operation<Void> original) {
        if(!NativeSelfCasts.failed(spell,entity))original.call(spell,world,level,entity,data);
    }
}
