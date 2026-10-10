package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.spells.ender.ArcaneShackleSpell;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = ArcaneShackleSpell.class, remap = false)
public interface ArcaneShackleAccess {
    @Invoker("getChainHealth") float ironsNouveau$health(int level, LivingEntity caster);
    @Invoker("getChainDuration") int ironsNouveau$duration(int level, LivingEntity caster);
    @Invoker("getLashRadius") float ironsNouveau$radius(int level, LivingEntity caster);
}
