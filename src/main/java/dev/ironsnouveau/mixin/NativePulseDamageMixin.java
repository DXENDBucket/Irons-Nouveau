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
        var motion = dev.ironsnouveau.casting.MotionEffects.current();
        if (motion != null && source.getEntity() == motion.actor()) {
            if (target == motion.actor() || target == motion.owner() || motion.owner().isAlliedTo(target)) return false;
            return original.call(target, amount * motion.damageScale(), motion.spell().getDamageSource(motion.actor(), motion.owner()));
        }
        var context = dev.arsconflux.api.context.CastContexts.current();
        // Native onCast implementations can construct their own source from the original caster.
        // Reattribute that source while retaining Iron's spell identity and all post-hit metadata.
        if (context != null && context.damageOwner() != null && context.damageOwner() != context.caster()
                && source.getEntity() == context.caster() && source instanceof io.redspace.ironsspellbooks.damage.SpellDamageSource nativeSource) {
            var attributed = nativeSource.spell().getDamageSource(source.getDirectEntity(), context.damageOwner())
                    .setLifestealPercent(nativeSource.getLifestealPercent()).setFireTicks(nativeSource.getFireTime())
                    .setFreezeTicks(nativeSource.getFreezeTicks()).setIFrames(nativeSource.getIFrames());
            if (!nativeSource.isDirect()) attributed.indirect();
            source = attributed;
        }
        DamageSource attributedSource = source;
        boolean success = EffectResources.pulse(source.getDirectEntity(), () -> original.call(target, amount, attributedSource));
        if (success) ComplexImpacts.damaged(target, attributedSource);
        return success;
    }
}
