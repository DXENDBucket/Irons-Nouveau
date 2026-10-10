package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.entity.spells.poison_cloud.PoisonSplash;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = PoisonSplash.class, remap = false)
public abstract class PoisonSplashChildrenMixin {
    @WrapMethod(method = "createPoisonCloud")
    private void ironsNouveau$cloud(Operation<Void> original) {
        var splash = (PoisonSplash)(Object)this;
        EffectResources.children(splash, splash.getEffectDuration(), original::call);
    }
}
