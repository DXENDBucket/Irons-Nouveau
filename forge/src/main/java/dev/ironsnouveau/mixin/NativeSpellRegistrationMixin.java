package dev.ironsnouveau.mixin;
import dev.ironsnouveau.compat.NativeSpellIndex;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value=DeferredRegister.class,remap=false)
public abstract class NativeSpellRegistrationMixin {
    @Inject(method="register(Ljava/lang/String;Ljava/util/function/Supplier;)Lnet/minecraftforge/registries/RegistryObject;",at=@At("RETURN"))
    private void ironsNouveau$announce(CallbackInfoReturnable<RegistryObject<?>> cir) {
        NativeSpellIndex.announced(((DeferredRegister<?>)(Object)this).getRegistryName(),cir.getReturnValue().getId());
    }
}
