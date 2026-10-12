package dev.ironsnouveau.platform;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
public final class RayClips {
    public static HitResult trace(Level world,Entity entity,Vec3 start,Vec3 end){
        // Forge 1.20.1's Entity constructor explicitly accepts null, using CollisionContext.empty().
        return world.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,entity));
    }
    private RayClips(){}
}
