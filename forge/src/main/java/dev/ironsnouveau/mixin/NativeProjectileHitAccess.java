package dev.ironsnouveau.mixin;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Projectile.class)
public interface NativeProjectileHitAccess { @Invoker(value = "onHit", remap = true) void ironsNouveau$hit(HitResult hit); }
