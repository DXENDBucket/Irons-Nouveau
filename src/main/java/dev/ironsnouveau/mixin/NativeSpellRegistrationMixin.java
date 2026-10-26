package dev.ironsnouveau.mixin;
import dev.ironsnouveau.compat.NativeSpellIndex;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=DeferredRegister.class,remap=false)
public abstract class NativeSpellRegistrationMixin {
    @Inject(method="register(Ljava/lang/String;Ljava/util/function/Function;)Lnet/neoforged/neoforge/registries/DeferredHolder;",at=@At("RETURN"))
    private void ironsNouveau$announce(CallbackInfoReturnable<DeferredHolder<?,?>> cir) {
        NativeSpellIndex.announced(((DeferredRegister<?>)(Object)this).getRegistryName(),cir.getReturnValue().getId());
    }
}
