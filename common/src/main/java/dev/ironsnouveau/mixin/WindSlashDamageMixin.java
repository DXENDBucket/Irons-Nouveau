package dev.ironsnouveau.mixin;
import dev.ironsnouveau.compat.wind.WindCombat;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;
@Pseudo @Mixin(targets="net.raptorzizi.wind_spellbooks.entity.spells.iron_slash.IronSlashEntity",remap=false)
public abstract class WindSlashDamageMixin {
    @Shadow @Final private List<LivingEntity> hitEntities;
    @Inject(method="applyDamageToAll",at=@At("HEAD"))
    private void ironsNouveau$protect(CallbackInfo ci){hitEntities.removeIf(target->WindCombat.protectedTarget((Entity)(Object)this,target));}
}
