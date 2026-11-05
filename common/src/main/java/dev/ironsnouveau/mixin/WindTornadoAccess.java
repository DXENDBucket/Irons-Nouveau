package dev.ironsnouveau.mixin;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Invoker;
@Pseudo @Mixin(targets="net.raptorzizi.wind_spellbooks.spells.wind.TornadoSpell",remap=false)
public interface WindTornadoAccess {
    @Invoker("getDamage") float ironsNouveau$damage(int level,LivingEntity caster);
}
