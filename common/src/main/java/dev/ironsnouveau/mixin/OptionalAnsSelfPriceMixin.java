package dev.ironsnouveau.mixin;

import dev.ironsnouveau.casting.NativeSelfCasts;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** ANS correctly exempts NONE from its own payment; preserve the event price
 * for our separate trigger account instead of turning that price into zero. */
@Pseudo @Mixin(targets="com.otectus.arsnspells.casting.IronsCastPayments",remap=false)
public abstract class OptionalAnsSelfPriceMixin {
    @Inject(method="stateNativeCost(Lio/redspace/ironsspellbooks/api/events/SpellOnCastEvent;Lcom/otectus/arsnspells/contract/CostQuote;)V",at=@At("HEAD"),cancellable=true,require=0)
    private static void ironsNouveau$keepTriggerPrice(SpellOnCastEvent event,@Coerce Object quote,CallbackInfo ci) {
        if(NativeSelfCasts.ownsEvent(event))ci.cancel();
    }
}
