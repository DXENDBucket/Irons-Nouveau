package dev.ironsnouveau.mixin;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.ironsnouveau.casting.TriggerGeometry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value = SpellResolver.class, remap = false)
public abstract class ResolutionGeometryMixin {
    @Inject(method = "onResolveEffect", at = @At("HEAD"))
    private void ironsNouveau$snapshot(CallbackInfo ci) { TriggerGeometry.capture(((SpellResolver)(Object)this).spellContext); }
}
