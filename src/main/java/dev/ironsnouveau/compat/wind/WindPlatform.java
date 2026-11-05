package dev.ironsnouveau.compat.wind;
import dev.ironsnouveau.bridge.Resolution;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.network.casting.OnClientCastPacket;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.*;
import net.minecraft.world.phys.Vec3;
import net.raptorzizi.wind_spellbooks.registries.ModMobEffectRegistry;
public final class WindPlatform {
    public static void pushSound(ServerLevel world,Vec3 pos){world.playSound(null,pos.x,pos.y,pos.z,
            net.minecraft.sounds.SoundEvents.WIND_CHARGE_BURST.value(),net.minecraft.sounds.SoundSource.PLAYERS,3,.7f);}
    public static void clientCast(LivingEntity actor,AbstractSpell spell,int level,ICastData data){
        if(actor instanceof ServerPlayer player)net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player,
                new OnClientCastPacket(spell.getSpellId(),level,CastSource.NONE,data));
    }
    public static void scaleEffects(LivingEntity actor,Resolution ctx){
        var effect=switch(ctx.definition().spellId().getPath()){
            case "tailwind" -> ModMobEffectRegistry.TAILWIND;
            case "wind_jump" -> MobEffects.SLOW_FALLING;
            default -> (net.minecraft.core.Holder<MobEffect>)null;
        };
        if(effect==null||ctx.duration()==1)return;
        var old=actor.getEffect(effect);if(old==null)return;
        actor.removeEffect(effect);
        actor.addEffect(new MobEffectInstance(effect,EffectResources.ticks(old.getDuration()*ctx.duration()),
                old.getAmplifier(),old.isAmbient(),old.isVisible(),old.showIcon()),ctx.caster());
    }
    private WindPlatform(){}
}
