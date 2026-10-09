package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.spells.holy.SunbeamSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = SunbeamSpell.class, remap = false)
public interface SunbeamAccess {
    @Invoker("getDamage") float ironsNouveau$damage(int level, LivingEntity caster);
}
