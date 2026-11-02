package dev.ironsnouveau.ability;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.casting.*;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.ironsnouveau.config.SpellLevelConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/** Shared active-cast coordination; retain Iron timing attributes, settings and chant overlay. */
public final class AbilityCasting implements ActiveCastProvider {
    private static final PersistentCooldowns.Namespace STORE=PersistentCooldowns.register(
            dev.ironsnouveau.platform.Locations.id("irons_nouveau:item_abilities"),"irons_nouveau_ability_cooldowns");
    public static void register(){ActiveCasts.register(dev.ironsnouveau.platform.Locations.id("irons_nouveau:item_abilities"),new AbilityCasting());}
    private static Set<IronAbility> abilities(List<Spell> recipes){var set=EnumSet.noneOf(IronAbility.class);for(var s:recipes)for(var p:ArsSpellAccess.parts(s)){var a=IronAbility.of(p);if(a!=null)set.add(a);}return set;}
    @Override public boolean supports(Spell spell){return !abilities(List.of(spell)).isEmpty();}
    // ChantTiming includes these in the same Iron MAX/SUM group as scroll spells.
    @Override public boolean chantingEnabled(ActiveCastRequest r){return false;}
    private static boolean cooldowns(LivingEntity actor){return SpellLevelConfig.cooldownsEnabled()&&ActiveCasts.cooldownsEnabled(actor)
            &&(!(actor instanceof Player p)||!p.isCreative()||io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.get());}
    @Override public boolean allowed(ActiveCastRequest r){return rejection(r)==null;}
    @Override public CastRejection rejection(ActiveCastRequest r){
        if(!cooldowns(r.caster()))return null;
        for(var a:abilities(List.of(r.spell()))){long left=STORE.remaining(r.caster(),a.path);if(left>0)return CastRejection.cooldown(Component.translatable("ars_nouveau.glyph_name.glyph_"+a.path),left);}return null;
    }
    @Override public void released(ActiveCastRequest r,List<Spell> recipes){if(cooldowns(r.caster()))for(var a:abilities(recipes))STORE.extend(r.caster(),a.path,io.redspace.ironsspellbooks.api.util.Utils.applyCooldownReduction(a.cooldown,r.caster()));}
    @Override public List<CooldownQuote> cooldowns(ActiveCastRequest r){
        if(!cooldowns(r.caster()))return List.of();return abilities(List.of(r.spell())).stream().filter(a->a.cooldown>0)
                .map(a->new CooldownQuote(a.id(),CastingTiming.cooldownTicks(r.caster(),io.redspace.ironsspellbooks.api.util.Utils.applyCooldownReduction(a.cooldown,r.caster())),STORE.remaining(r.caster(),a.path))).toList();
    }
}
