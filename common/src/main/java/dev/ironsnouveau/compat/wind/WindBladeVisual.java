package dev.ironsnouveau.compat.wind;
import net.minecraft.world.entity.Entity;
import net.raptorzizi.wind_spellbooks.entity.spells.wind_blade.WindBladeProjectile;
/** Native visual growth and particles without a second server movement/collision driver. */
public final class WindBladeVisual {
    public static void tick(Entity entity){
        var blade=(WindBladeProjectile)entity;
        blade.setRadius(blade.getRadius()+.06f);
        if(entity.level().isClientSide){entity.setPos(entity.position().add(entity.getDeltaMovement()));blade.spawnParticles();}
    }
    private WindBladeVisual(){}
}
