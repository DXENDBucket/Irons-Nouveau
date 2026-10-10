package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.spells.lightning.ThunderstormSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = ThunderstormSpell.class, remap = false)
public interface ThunderstormSpellAccess {
    @Invoker("getAmplifierForLevel") int ironsNouveau$amplifier(int level, LivingEntity caster);
}
