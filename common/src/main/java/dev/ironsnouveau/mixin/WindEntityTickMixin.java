package dev.ironsnouveau.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.arsconflux.api.damage.*;
import dev.ironsnouveau.casting.EffectResources;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
@Pseudo @Mixin(targets={"net.raptorzizi.wind_spellbooks.entity.spells.tornado.TornadoEntity",
        "net.raptorzizi.wind_spellbooks.entity.spells.almighty_push.AlmightyPushEntity",
        "net.raptorzizi.wind_spellbooks.entity.spells.iron_slash.IronSlashEntity"},remap=false)
public abstract class WindEntityTickMixin {
    @WrapMethod(method={"tick","m_8119_"},remap=false)
    private void ironsNouveau$windTick(Operation<Void> original){
        var entity=(Entity)(Object)this;
        if(!EffectResources.managed(entity)){original.call();return;}
        var profile=SpellPower.profile(entity);
        if(profile==null){original.call();return;}
        var attached=NativeSpellDamage.context(entity);
        var frame=attached!=null?attached:dev.arsconflux.api.context.CastContext.detached(profile.caster(),null,null)
                .withExecutor(profile.executor()).withDamageOwner(profile.damageOwner());
        SpellPower.tick(entity,()->NativeSpellDamage.during(frame,profile.schools(),()->{original.call();return null;}));
    }
}
