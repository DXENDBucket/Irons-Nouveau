package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.spells.blood.BloodStepSpell;
import io.redspace.ironsspellbooks.spells.ice.FrostStepSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = {BloodStepSpell.class, FrostStepSpell.class}, remap = false)
public interface StepDistanceAccess {
    @Invoker("getDistance") float ironsNouveau$distance(int level, LivingEntity caster);
}
