package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.ironsnouveau.compat.wind.WindNativeAction;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
@Pseudo @Mixin(targets="net.raptorzizi.wind_spellbooks.spells.wind.IronSlashSpell",remap=false)
public abstract class WindWeaponPowerMixin {
    @WrapOperation(method="getDamage",at=@At(value="INVOKE",target="Lio/redspace/ironsspellbooks/api/util/Utils;getWeaponDamage(Lnet/minecraft/world/entity/LivingEntity;)F"))
    private float ironsNouveau$weapon(LivingEntity actor,Operation<Float> original){
        var ctx=WindNativeAction.current();
        if(ctx!=null&&ctx.caster().getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)==null)return 0;
        return original.call(ctx==null?actor:ctx.caster());
    }
}
