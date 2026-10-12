package dev.ironsnouveau.casting;

import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import java.util.function.Predicate;

/** Search the actual ray, retaining Iron's intersection routine and vanilla block clipping. */
public final class RayGeometry {
    public static HitResult trace(Level world,Entity anchor,Vec3 start,Vec3 end,float inflation,Predicate<Entity> eligible) {
        HitResult nearest=dev.ironsnouveau.platform.RayClips.trace(world,anchor,start,end);
        Vec3 clipped=nearest.getLocation();double distance=start.distanceToSqr(clipped);
        for(var entity:world.getEntities((Entity)null,new AABB(start,clipped).inflate(inflation),eligible)) {
            var hit=Utils.checkEntityIntersecting(entity,start,clipped,inflation);
            double candidate=start.distanceToSqr(hit.getLocation());
            if(hit.getType()!=HitResult.Type.MISS && candidate<distance){nearest=hit;distance=candidate;}
        }
        return nearest;
    }
    private RayGeometry(){}
}
