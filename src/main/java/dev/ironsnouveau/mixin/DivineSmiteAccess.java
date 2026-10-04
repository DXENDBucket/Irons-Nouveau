package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.spells.holy.DivineSmiteSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = DivineSmiteSpell.class, remap = false)
public interface DivineSmiteAccess {
    @Invoker("getDamage") float ironsNouveau$damage(int level, LivingEntity caster);
}
