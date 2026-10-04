package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.spells.eldritch.SculkTentaclesSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = SculkTentaclesSpell.class, remap = false)
public interface SculkSpellAccess {
    @Invoker("getDamage") float ironsNouveau$damage(int level, LivingEntity caster);
}
