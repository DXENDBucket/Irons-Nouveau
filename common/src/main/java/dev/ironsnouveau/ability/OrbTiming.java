package dev.ironsnouveau.ability;
import dev.ironsnouveau.casting.EffectResources;
import io.redspace.ironsspellbooks.entity.mobs.wizards.fire_boss.fire_orb.OminousFireOrbEntity;
public final class OrbTiming {
    public static void beforeTick(OminousFireOrbEntity orb){
        var clock=(OrbClock)orb;long origin=clock.ironsNouveau$startedAt();
        if(!orb.level().isClientSide&&origin<0&&EffectResources.ability(orb)==IronAbility.OMINOUS_FIRE_ORB){
            // Native NBT restores Age; a newly loaded entity resumes that age without forcing chunks.
            origin=orb.level().getGameTime()-orb.tickCount;clock.ironsNouveau$startedAt(origin);
        }
        if(origin>=0)orb.tickCount=(int)Math.max(0,Math.min(Integer.MAX_VALUE-1L,orb.level().getGameTime()-origin));
    }
    private OrbTiming(){}
}
