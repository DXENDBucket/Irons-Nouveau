package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.world.entity.Entity;
import dev.ironsnouveau.compat.wind.WindNativeAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Only the scoped native Wind callback substitutes its original power provider. */
@Mixin(value=AbstractSpell.class,remap=false)
public abstract class WindPowerCasterMixin {
    @ModifyVariable(method="getSpellPower",at=@At("HEAD"),argsOnly=true)
    private Entity ironsNouveau$windPower(Entity supplied){return WindNativeAction.powerCaster(this,supplied);}
}
