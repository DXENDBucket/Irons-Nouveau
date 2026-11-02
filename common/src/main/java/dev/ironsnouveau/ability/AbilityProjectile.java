package dev.ironsnouveau.ability;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.ResourceCharge;
import java.util.List;

public final class AbilityProjectile extends AbstractAugment implements ProjectileForm {
    public final IronAbility ability=IronAbility.FIERY_DAGGER;
    public AbilityProjectile(){super(IronAbility.FIERY_DAGGER.glyphId(),IronAbility.FIERY_DAGGER.name);}
    @Override public boolean replace(EntityProjectileSpell carrier,CarrierProfile profile){return DaggerFlight.replace(carrier,profile);}
    @Override public ResourceCharge initialCharge(SpellContext context,List<AbstractAugment> augments){
        var frame=dev.arsconflux.api.context.CastContext.of(context,null,null);
        return frame.caster()==null?null:new ResourceCharge(dev.ironsnouveau.casting.TriggerMana.of(frame).account(),ability.mana);
    }
    @Override public int getDefaultManaCost(){return 0;}
    @Override public int getCastingCost(){return 0;}
    @Override public SpellTier defaultTier(){return SpellTier.ONE;}
    @Override public String getBookDescription(){return net.minecraft.network.chat.Component.translatable("irons_nouveau.ability."+getRegistryName().getPath()).getString();}
}
