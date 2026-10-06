package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.*;
import dev.ironsnouveau.casting.ComplexImpacts;
import io.redspace.ironsspellbooks.entity.spells.acid_orb.AcidOrb;
import io.redspace.ironsspellbooks.entity.spells.snowball.Snowball;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value = {AcidOrb.class, Snowball.class}, remap = false)
public abstract class ComplexStatusImpactMixin {
    @WrapOperation(method = "onHit", remap = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z", remap = true))
    private boolean ironsNouveau$status(LivingEntity target, MobEffectInstance effect, Operation<Boolean> original) {
        boolean applied = original.call(target, effect);
        if (applied) ComplexImpacts.statusApplied(target);
        return applied;
    }
}
