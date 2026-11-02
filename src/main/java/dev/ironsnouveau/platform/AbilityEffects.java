package dev.ironsnouveau.platform;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.Level;
public final class AbilityEffects {
    public static void gust(ServerLevel world,Vec3 pos){
        world.explode(null,null,null,pos.x,pos.y,pos.z,0,false,Level.ExplosionInteraction.NONE,
                ParticleTypes.GUST_EMITTER_SMALL,ParticleTypes.GUST_EMITTER_LARGE,SoundEvents.WIND_CHARGE_BURST);
    }
    private AbilityEffects(){}
}
