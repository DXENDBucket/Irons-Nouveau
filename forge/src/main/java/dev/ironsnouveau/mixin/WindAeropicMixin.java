package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.MotionEffects;
import dev.ironsnouveau.platform.Locations;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.*;
@Pseudo @Mixin(targets="net.raptorzizi.wind_spellbooks.effect.AeropicEffect",remap=false)
public abstract class WindAeropicMixin {
    @WrapMethod(method={"applyEffectTick","m_6742_"},remap=false)
    private void ironsNouveau$owner(LivingEntity actor,int amplifier,Operation<Void> original){
        MotionEffects.tick(actor,Locations.id("wind_spellbooks:aeropic"),()->
                dev.ironsnouveau.compat.wind.WindNativeAction.motionDamage(()->{original.call(actor,amplifier);return true;}));
    }
}
