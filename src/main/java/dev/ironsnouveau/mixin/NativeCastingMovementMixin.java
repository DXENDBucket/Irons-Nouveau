package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.ironsnouveau.client.CastingMovementState;
import io.redspace.ironsspellbooks.player.ClientPlayerEvents;
import org.spongepowered.asm.mixin.Mixin;

/** Bridge preparation uses the native attribute formula, composed once with other providers. */
@Mixin(value=ClientPlayerEvents.class,remap=false)
public abstract class NativeCastingMovementMixin {
    @WrapMethod(method="onCalculatePlayerSpeed")
    private static void ironsNouveau$movement(net.neoforged.neoforge.client.event.MovementInputUpdateEvent event,Operation<Void> original) {
        if(!CastingMovementState.active()) {original.call(event);return;}
        float nativeScale=.2f+(float)event.getEntity().getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CASTING_MOVESPEED)-1;
        float ratio=dev.arsconflux.api.casting.MovementLimits.multiplier(event,nativeScale);
        event.getInput().leftImpulse*=ratio;event.getInput().forwardImpulse*=ratio;
    }
}
