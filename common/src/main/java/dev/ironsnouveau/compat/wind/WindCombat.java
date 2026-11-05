package dev.ironsnouveau.compat.wind;
import dev.arsconflux.api.damage.SpellPower;
import dev.arsconflux.api.targeting.Allies;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
public final class WindCombat {
    public static boolean protectedTarget(Entity payload,Entity target){
        if(payload==null||!payload.getPersistentData().getCompound("irons_nouveau_lease").getString("spell").startsWith("wind_spellbooks:"))return false;
        var profile=SpellPower.profile(payload);
        return profile!=null&&(target==profile.executor()||Allies.areAllies(profile.caster(),target));
    }
    private WindCombat(){}
}
