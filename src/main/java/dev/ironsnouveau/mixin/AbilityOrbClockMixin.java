package dev.ironsnouveau.mixin;
import dev.ironsnouveau.ability.*;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.fire_orb.OminousFireOrbEntity;
import net.minecraft.network.syncher.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=OminousFireOrbEntity.class,remap=false)
public abstract class AbilityOrbClockMixin implements OrbClock {
    @Unique private static final EntityDataAccessor<Long> ironsNouveau$START=SynchedEntityData.defineId(OminousFireOrbEntity.class,EntityDataSerializers.LONG);
    @Inject(method="defineSynchedData",remap=true,at=@At("TAIL"))
    private void ironsNouveau$define(SynchedEntityData.Builder builder,CallbackInfo ci){builder.define(ironsNouveau$START,-1L);}
    public long ironsNouveau$startedAt(){return ((OminousFireOrbEntity)(Object)this).getEntityData().get(ironsNouveau$START);}
    public void ironsNouveau$startedAt(long tick){((OminousFireOrbEntity)(Object)this).getEntityData().set(ironsNouveau$START,tick);}
    @Inject(method="tick",remap=true,at=@At("HEAD"))
    private void ironsNouveau$clock(CallbackInfo ci){OrbTiming.beforeTick((OminousFireOrbEntity)(Object)this);}
}
