package dev.ironsnouveau.platform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
public final class AbilityEffects {
    public static void gust(ServerLevel world,Vec3 pos){
        // Wind-charge particles do not exist in 1.20.1; retain visible gust and native sound.
        world.sendParticles(ParticleTypes.CLOUD,pos.x,pos.y+.5,pos.z,35,.5,.3,.5,.15);
        dev.ironsnouveau.ability.AbilityPayloads.sound(world,pos,"minecraft:entity.generic.explode",.5f,1.6f);
    }
    private AbilityEffects(){}
}
