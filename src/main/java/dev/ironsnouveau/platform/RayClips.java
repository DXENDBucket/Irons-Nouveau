package dev.ironsnouveau.platform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.phys.shapes.CollisionContext;
public final class RayClips {
    public static HitResult trace(Level world,Entity entity,Vec3 start,Vec3 end){
        return world.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,
                entity==null?CollisionContext.empty():CollisionContext.of(entity)));
    }
    private RayClips(){}
}
