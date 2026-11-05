package dev.ironsnouveau.compat.wind;
import dev.ironsnouveau.casting.NativeCastCarrier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
public interface WindBladeState extends NativeCastCarrier {
    void ironsNouveau$damage(Entity target);
}
