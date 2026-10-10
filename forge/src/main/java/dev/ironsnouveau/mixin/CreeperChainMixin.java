package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.entity.spells.creeper_head.CreeperHeadProjectile;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
@Mixin(value = CreeperHeadProjectile.class, remap = false)
public abstract class CreeperChainMixin {
    @WrapOperation(method = "onHit", remap = true, at = @At(value = "INVOKE", target = "Lio/redspace/ironsspellbooks/spells/evocation/ChainCreeperSpell;summonCreeperRing(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/phys/Vec3;FI)V", remap = false))
    private void ironsNouveau$chain(Level level, LivingEntity owner, Vec3 pos, float damage, int count, Operation<Void> original) {
        EffectResources.emit((CreeperHeadProjectile)(Object)this, 300, false,
                () -> original.call(level, owner, pos, damage, count));
    }
}
