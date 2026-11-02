package dev.ironsnouveau.mixin;
import dev.ironsnouveau.ability.WallLifetime;
import io.redspace.ironsspellbooks.entity.spells.wall_of_fire.WallOfFireEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(value=WallOfFireEntity.class,remap=false)
public interface AbilityWallMixin extends WallLifetime {
    @Override @Accessor("lifetime") void ironsNouveau$lifetime(int ticks);
}
