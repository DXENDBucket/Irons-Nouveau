package dev.ironsnouveau.ability;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import java.util.*;

public final class AbilityCatalog {
    private static final List<AbstractSpellPart> PARTS=new ArrayList<>();
    public static final AbilityProjectile DAGGER=new AbilityProjectile();
    public static void register(){
        for(var a:IronAbility.values()){
            AbstractSpellPart part=a==IronAbility.FIERY_DAGGER?DAGGER:new AbilityGlyph(a);
            GlyphRegistry.registerSpell(part);PARTS.add(part);
        }
        AbilityCasting.register();
    }
    public static List<AbstractSpellPart> parts(){return List.copyOf(PARTS);}
    public static void carriers(){for(var p:dev.arsconflux.api.projectile.CarrierRegistry.profiles())if(p.glyph()!=null)p.glyph().compatibleAugments.add(DAGGER);}
    private AbilityCatalog(){}
}
