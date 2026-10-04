package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = AbstractMagicProjectile.class, remap = false)
public interface NativeProjectileHitAccess { @Invoker("onHit") void ironsNouveau$hit(HitResult hit); }
