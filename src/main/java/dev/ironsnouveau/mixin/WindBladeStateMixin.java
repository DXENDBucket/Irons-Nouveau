package dev.ironsnouveau.mixin;
import dev.ironsnouveau.casting.CastSession;
import dev.ironsnouveau.compat.wind.WindBladeState;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Pseudo @Mixin(targets="net.raptorzizi.wind_spellbooks.entity.spells.wind_blade.WindBladeProjectile",remap=false)
public abstract class WindBladeStateMixin implements WindBladeState {
    @Unique private CastSession ironsNouveau$session;
    @Unique private static final EntityDataAccessor<Boolean> ironsNouveau$driven=SynchedEntityData.defineId(
            net.raptorzizi.wind_spellbooks.entity.spells.wind_blade.WindBladeProjectile.class,EntityDataSerializers.BOOLEAN);
    @Inject(method="defineSynchedData",at=@At("TAIL"),remap=true)
    private void ironsNouveau$data(SynchedEntityData.Builder builder,CallbackInfo ci){
        builder.define(ironsNouveau$driven,false);
    }
    @Override public CastSession ironsNouveau$session(){return ironsNouveau$session;}
    @Override public void ironsNouveau$session(CastSession value){ironsNouveau$session=value;}
    @Override public boolean ironsNouveau$arsDriven(){return ((Entity)(Object)this).getEntityData().get(ironsNouveau$driven);}
    @Override public void ironsNouveau$arsDriven(boolean value){((Entity)(Object)this).getEntityData().set(ironsNouveau$driven,value);}
}
