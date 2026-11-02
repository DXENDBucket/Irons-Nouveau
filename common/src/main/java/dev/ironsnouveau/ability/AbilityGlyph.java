package dev.ironsnouveau.ability;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.glyph.TriggerPaidGlyph;
import dev.arsconflux.api.resource.ResourceCharge;
import dev.ironsnouveau.casting.TriggerMana;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import java.util.*;

public final class AbilityGlyph extends AbstractEffect implements TriggerPaidGlyph {
    public final IronAbility ability;
    public AbilityGlyph(IronAbility ability){super(ability.glyphId(),ability.name);this.ability=ability;}
    @Override public void onResolve(HitResult hit,Level level,LivingEntity ignored,SpellStats stats,SpellContext context,SpellResolver resolver){
        if(!(level instanceof ServerLevel world)||!isEnabled())return;
        var frame=CastContext.of(context,hit,null);
        if(frame.caster()==null||!frame.caster().isAlive()||!world.hasChunkAt(net.minecraft.core.BlockPos.containing(hit.getLocation())))return;
        var mana=TriggerMana.of(frame);var paid=frame.withAccount(mana.account());
        firstCharge(context,stats.getAugments()).trigger(()->CastContexts.scoped(paid,
                ()->AbilityPayloads.apply(ability,world,paid,stats)));
    }
    @Override public ResourceCharge firstCharge(SpellContext context,List<AbstractAugment> augments){
        var frame=CastContext.of(context,null,null);return frame.caster()==null?null:new ResourceCharge(TriggerMana.of(frame).account(),ability.mana);
    }
    @Override public int getDefaultManaCost(){return 0;}
    @Override public int getCastingCost(){return 0;}
    @Override public SpellTier defaultTier(){return SpellTier.ONE;}
    @Override protected Set<AbstractAugment> getCompatibleAugments(){
        // Ars calls this from its constructor, before our ability field is assigned.
        if(getRegistryName().equals(IronAbility.EXPULSION.glyphId()))return Set.of(AugmentAmplify.INSTANCE,AugmentDampen.INSTANCE,AugmentAOE.INSTANCE);
        if(getRegistryName().equals(IronAbility.UNDEAD_RIFT.glyphId()))return Set.of(AugmentAmplify.INSTANCE,AugmentDampen.INSTANCE,AugmentExtendTime.INSTANCE,AugmentDurationDown.INSTANCE);
        return Set.of(AugmentAmplify.INSTANCE,AugmentDampen.INSTANCE,AugmentAOE.INSTANCE,AugmentExtendTime.INSTANCE,AugmentDurationDown.INSTANCE);
    }
    @Override public String getBookDescription(){return net.minecraft.network.chat.Component.translatable("irons_nouveau.ability."+getRegistryName().getPath()).getString();}
    public void addAugmentDescriptions(Map<AbstractAugment,String> descriptions){
        describe(descriptions,AugmentAmplify.INSTANCE,"amplify");
        describe(descriptions,AugmentDampen.INSTANCE,"dampen");
        describe(descriptions,AugmentAOE.INSTANCE,"area");
        describe(descriptions,AugmentExtendTime.INSTANCE,"extend");
        describe(descriptions,AugmentDurationDown.INSTANCE,"shorten");
    }
    private void describe(Map<AbstractAugment,String> out,AbstractAugment augment,String key){
        if(getCompatibleAugments().contains(augment))out.put(augment,net.minecraft.network.chat.Component.translatable("irons_nouveau.ability.augment."+key).getString());
    }
}
