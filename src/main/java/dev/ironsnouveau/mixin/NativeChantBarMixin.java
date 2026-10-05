package dev.ironsnouveau.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.ironsnouveau.client.ChantHudState;
import io.redspace.ironsspellbooks.api.spells.CastType;
import io.redspace.ironsspellbooks.gui.overlays.CastBarOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Reuse Iron's actual renderer, textures and time formatting, scoped strictly to this overlay. */
@Mixin(value = CastBarOverlay.class, remap = false)
public abstract class NativeChantBarMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/player/ClientMagicData;isCasting()Z"))
    private boolean ironsNouveau$active(Operation<Boolean> original) {
        return original.call() || ChantHudState.visible();
    }
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/player/ClientMagicData;getCastType()Lio/redspace/ironsspellbooks/api/spells/CastType;"))
    private CastType ironsNouveau$type(Operation<CastType> original) {
        return ChantHudState.visible() ? CastType.LONG : original.call();
    }
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/player/ClientMagicData;getCastCompletionPercent()F"))
    private float ironsNouveau$progress(Operation<Float> original) {
        return ChantHudState.visible() ? ChantHudState.progress() : original.call();
    }
    @WrapOperation(method = "render", at = @At(value = "INVOKE",
            target = "Lio/redspace/ironsspellbooks/player/ClientMagicData;getCastDuration()I"))
    private int ironsNouveau$duration(Operation<Integer> original) {
        return ChantHudState.visible() ? ChantHudState.duration() : original.call();
    }
}
