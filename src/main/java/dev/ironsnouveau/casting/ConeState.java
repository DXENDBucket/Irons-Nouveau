package dev.ironsnouveau.casting;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.*;
import java.util.Set;
/** Networked emission pose is separate from Projectile.owner; bound offsets are relative to anchor eyes. */
public interface ConeState {
    boolean ironsNouveau$managed();
    int ironsNouveau$anchor();
    Vec3 ironsNouveau$offset();
    float ironsNouveau$scale();
    void ironsNouveau$bind(int anchor, Vec3 offset, float scale);
    Set<Entity> ironsNouveau$targets();
    void ironsNouveau$hit(EntityHitResult hit);
}
