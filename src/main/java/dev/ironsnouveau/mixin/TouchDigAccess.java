package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.spells.nature.TouchDigSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = TouchDigSpell.class, remap = false)
public interface TouchDigAccess {
    @Invoker("canBreak") boolean ironsNouveau$canBreak(Level level, BlockPos pos, double power);
    @Invoker("doDestroyBlock") void ironsNouveau$destroy(Level level, BlockPos pos, LivingEntity caster);
}
