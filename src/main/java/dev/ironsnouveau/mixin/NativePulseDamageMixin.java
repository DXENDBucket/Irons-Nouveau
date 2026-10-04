package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.EffectResources;
import dev.ironsnouveau.casting.ComplexImpacts;
import io.redspace.ironsspellbooks.damage.DamageSources;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
@Mixin(value = DamageSources.class, remap = false)
public abstract class NativePulseDamageMixin {
    @WrapMethod(method = "applyDamage")
    private static boolean ironsNouveau$pulse(Entity target, float amount, DamageSource source, Operation<Boolean> original) {
        boolean success = EffectResources.pulse(source.getDirectEntity(), () -> original.call(target, amount, source));
        if (success) ComplexImpacts.damaged(target, source);
        return success;
    }
}
