package dev.ironsnouveau.mixin;

import io.redspace.ironsspellbooks.spells.fire.WallOfFireSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = WallOfFireSpell.class, remap = false)
public interface WallOfFireAccess {
    @Invoker("getWallLength") float ironsNouveau$length(int level, LivingEntity caster);
    @Invoker("getDamage") float ironsNouveau$damage(int level, LivingEntity caster);
    @Invoker("setOnGround") Vec3 ironsNouveau$ground(Vec3 position, Level world);
}
