package dev.ironsnouveau.mixin;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.gen.Accessor;
@Pseudo @Mixin(targets="net.raptorzizi.wind_spellbooks.entity.spells.iron_slash.IronSlashEntity",remap=false)
public interface WindSlashAccess {
    @Accessor("caster") void ironsNouveau$owner(LivingEntity caster);
}
