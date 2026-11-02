package dev.ironsnouveau.ability;

import net.minecraft.world.entity.LivingEntity;

public final class AbilityPlatform {
    public static int chant(LivingEntity actor,int ticks){
        var reference=io.redspace.ironsspellbooks.api.registry.SpellRegistry.FIREBALL_SPELL.get();
        return Math.max(0,Math.round(ticks*(float)reference.getEffectiveCastTime(1,actor)/Math.max(1,reference.getCastTime(1))));
    }
    private AbilityPlatform(){}
}
