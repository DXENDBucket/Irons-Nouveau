package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.spells.ice.SummonPolarBearSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = SummonPolarBearSpell.class, remap = false)
public interface PolarBearSpellAccess {
    @Invoker("getBearHealth") float ironsNouveau$health(int level, LivingEntity caster);
}
