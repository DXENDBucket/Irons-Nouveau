package dev.ironsnouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.casting.NativeRayGeometry;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.util.RaycastBuilder;
import io.redspace.ironsspellbooks.spells.ice.RayOfFrostSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(value = RayOfFrostSpell.class, remap = false)
public abstract class NativeFrostRayMixin {
    @WrapOperation(method = "onCast", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/util/RaycastBuilder;build()Lnet/minecraft/world/phys/HitResult;"))
    private HitResult ironsNouveau$hit(RaycastBuilder builder, Operation<HitResult> original) {
        var view = NativeRayGeometry.current();
        return view == null ? original.call(builder) : view.hit();
    }
    @WrapOperation(method = "onCast", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;getEyePosition()Lnet/minecraft/world/phys/Vec3;", remap = true))
    private Vec3 ironsNouveau$origin(LivingEntity caster, Operation<Vec3> original) {
        var view = NativeRayGeometry.current();
        return view == null ? original.call(caster) : view.origin();
    }
    @WrapOperation(method = "onCast", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/api/spells/AbstractSpell;onCast(Lnet/minecraft/world/level/Level;ILnet/minecraft/world/entity/LivingEntity;Lio/redspace/ironsspellbooks/api/spells/CastSource;Lio/redspace/ironsspellbooks/api/magic/MagicData;)V"))
    private void ironsNouveau$sound(RayOfFrostSpell spell, Level world, int level, LivingEntity caster,
                                    CastSource source, MagicData data, Operation<Void> original) {
        // BridgeGlyph already plays the finish sound at the Ars trigger position.
        if (NativeRayGeometry.current() == null) original.call(spell, world, level, caster, source, data);
    }
}
