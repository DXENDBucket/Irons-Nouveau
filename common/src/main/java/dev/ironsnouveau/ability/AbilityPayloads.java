package dev.ironsnouveau.ability;

import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.damage.SpellPower;
import dev.arsconflux.api.school.SchoolDamage;
import dev.arsconflux.api.targeting.Allies;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.api.util.Utils;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.undead_spawner.UndeadRiftEntity;
import io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity;
import io.redspace.ironsspellbooks.entity.spells.wall_of_fire.WallOfFireEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.*;
import java.util.*;

public final class AbilityPayloads {
    public static final int SUMMON_LIMIT=12;
    public static boolean hasSummonRoom(ServerLevel world,LivingEntity owner){
        return SummonManager.getSummons(owner).stream().filter(id->{var e=world.getEntity(id);return !(e instanceof UndeadRiftEntity);}).count()<SUMMON_LIMIT;
    }
    public static double duration(SpellStats stats){return dev.ironsnouveau.bridge.AugmentScaling.duration(stats.getDurationMultiplier());}
    public static float damage(LivingEntity caster,double amplify,float base){
        double nativeScale=SpellRegistry.FLAMING_STRIKE_SPELL.get().getEntityPowerMultiplier(caster);
        return (float)Math.max(0,(base+2*amplify)*nativeScale);
    }
    public static boolean apply(IronAbility a,ServerLevel world,CastContext frame,SpellStats stats){
        var hit=frame.target();if(hit==null)return false;
        var center=hit.getLocation();
        if(hit instanceof EntityHitResult e)center=e.getEntity().position();
        if(!world.hasChunkAt(BlockPos.containing(center)))return false;
        var point=center;var profile=new SpellPower.Profile(frame,SchoolDamage.current());
        return SpellPower.scoped(profile,()->{
            switch(a){
                case UNDEAD_RIFT -> {
                    if(!hasSummonRoom(world,frame.caster()))return false;
                    var pos=Utils.moveToRelativeGroundLevel(world,point,4,10);
                    if(!world.hasChunkAt(BlockPos.containing(pos)))return false;
                    var rift=new UndeadRiftEntity(world);rift.moveTo(pos);rift.setDelay(20);
                    rift.setSummonsToSpawn((int)Math.max(1,Math.min(12,5+stats.getAmpMultiplier())));
                    SummonManager.setOwner(rift,frame.caster());
                    if(hit instanceof EntityHitResult e&&e.getEntity() instanceof LivingEntity enemy&&!Allies.areAllies(frame.caster(),enemy))rift.setForcedTarget(enemy);
                    rift.setYRot(frame.executor()==null?frame.caster().getYHeadRot():frame.executor().getYHeadRot());
                    rift.getPersistentData().putInt("irons_nouveau_summon_duration",EffectResources.ticks(6000*duration(stats)));
                    if(!EffectResources.spawnAbility(a,world,frame,rift,600,false)){SummonManager.removeSummon(rift);return false;}
                    world.sendParticles(ParticleTypes.LARGE_SMOKE,pos.x,pos.y+1,pos.z,35,.1,.3,.1,.1);
                    sound(world,pos,"minecraft:entity.vex.ambient",1,.75f);return true;
                }
                case DAGGER_ZONE -> {
                    var dagger=new FieryDaggerEntity(world);dagger.setOwner(frame.damageOwner());dagger.moveTo(point);
                    dagger.setDamage(damage(frame.caster(),stats.getAmpMultiplier(),7.5f));
                    dagger.setExplosionRadius((float)Math.max(1,Math.min(12,5+stats.getAoeMultiplier())));
                    dagger.delay=EffectResources.ticks(20*duration(stats));
                    dagger.getPersistentData().putDouble("irons_nouveau_duration_scale",duration(stats));
                    if(!EffectResources.spawnAbility(a,world,frame,dagger,600,true))return false;
                    // The complete native method also creates the rising blades and FireField.
                    EffectResources.prepaidChildren(dagger,EffectResources.ticks(600*duration(stats)),()->dagger.createDaggerZone(point));
                    dagger.discard();return true;
                }
                case RING_OF_FIRE -> {
                    double radius=Math.max(1,Math.min(16,5+stats.getAoeMultiplier()));
                    var anchors=new ArrayList<Vec3>();
                    for(int i=0;i<=8;i++){
                        var candidate=point.add(Math.cos(i*Math.PI/4)*radius,0,Math.sin(i*Math.PI/4)*radius);
                        if(!world.hasChunkAt(BlockPos.containing(candidate)))return false;
                        anchors.add(Utils.moveToRelativeGroundLevel(world,candidate,8));
                    }
                    var wall=new WallOfFireEntity(world,frame.damageOwner(),anchors,damage(frame.caster(),stats.getAmpMultiplier(),10));
                    wall.setPos(point);int ticks=EffectResources.ticks(240*duration(stats));
                    ((WallLifetime)wall).ironsNouveau$lifetime(ticks);
                    return EffectResources.spawnAbility(a,world,frame,wall,ticks+2,false);
                }
                case EXPULSION -> {
                    double radius=Math.max(1,Math.min(16,4+stats.getAoeMultiplier()));
                    for(var entity:world.getEntities(frame.caster(),new AABB(point,point).inflate(radius),e->e.isAlive()&&!e.isSpectator()&&!Allies.areAllies(frame.caster(),e))){
                        double d=entity.distanceToSqr(point);if(d>=radius*radius)continue;
                        double resistance=entity instanceof LivingEntity living?1-living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE):1;
                        var delta=entity.getBoundingBox().getCenter().subtract(point).normalize().add(0,.1,0)
                                .scale(Math.max(0,1-d/(radius*radius)+.6*resistance)*Math.max(.1,1+.2*stats.getAmpMultiplier()));
                        entity.setDeltaMovement(entity.getDeltaMovement().add(delta));entity.hurtMarked=true;
                        if(entity instanceof net.minecraft.server.level.ServerPlayer player)player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(entity));
                    }
                    dev.ironsnouveau.platform.AbilityEffects.gust(world,point);return true;
                }
                default -> {return false;}
            }
        });
    }
    public static void sound(ServerLevel world,Vec3 point,String id,float volume,float pitch){
        var sound=BuiltInRegistries.SOUND_EVENT.get(dev.ironsnouveau.platform.Locations.id(id));
        if(sound!=null)world.playSound(null,BlockPos.containing(point),sound,SoundSource.PLAYERS,volume,pitch);
    }
    private AbilityPayloads(){}
}
