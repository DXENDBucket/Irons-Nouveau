package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.casting.*;
import dev.ironsnouveau.config.SpellLevelConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Iron owns only its native rules and presentation; the active-use transaction belongs to Conflux. */
public final class IronActiveCastProvider implements ActiveCastProvider {
    private static final PersistentCooldowns.Namespace MOB_COOLDOWNS=PersistentCooldowns.register(
            dev.ironsnouveau.platform.Locations.id("irons_nouveau:mob_casting"),"irons_nouveau_mob_cooldowns");
    public static void register() { ActiveCasts.register(dev.ironsnouveau.platform.Locations.id("irons_nouveau:active_casting"), new IronActiveCastProvider()); }
    @Override public boolean supports(Spell spell) { return ChantTiming.containsIron(spell); }
    @Override public boolean chantingEnabled(ActiveCastRequest request) { return SpellLevelConfig.chantingEnabled(); }
    @Override public List<Integer> chantTicks(ActiveCastRequest request) {
        var ticks = ChantTiming.occurrences(request.spell(), request.caster());
        // Preserve the existing Iron-only SUM setting as an explicit provider-group override.
        return SpellLevelConfig.chantMode() == SpellLevelConfig.ChantMode.SUM
                ? List.of(ChantDurations.combine(ticks, true)) : ticks;
    }
    private boolean usesCooldowns(ActiveCastRequest request) {
        if(!SpellLevelConfig.cooldownsEnabled())return false;
        if(!(request.caster() instanceof net.minecraft.world.entity.player.Player))return true;
        return request.caster() instanceof ServerPlayer player&&!dev.ironsnouveau.platform.Casters.isFake(player)
                &&(!player.isCreative()||ServerConfigs.CREATIVE_COOLDOWN.get());
    }
    private static Set<AbstractSpell> spells(List<Spell> recipes) {
        var result = new LinkedHashSet<AbstractSpell>();
        for (var recipe : recipes) for (var part : dev.arsconflux.api.glyph.ArsSpellAccess.parts(recipe)) {
            var id = BoundSpellSupport.ironSpell(part);
            if (id == null) continue;
            var nativeSpell = SpellRegistry.getSpell(id);
            if (nativeSpell != SpellRegistry.none()) result.add(nativeSpell);
        }
        return result;
    }
    @Override public boolean allowed(ActiveCastRequest request) { return rejection(request)==null; }
    @Override public dev.arsconflux.api.casting.CastRejection rejection(ActiveCastRequest request) {
        if(!usesCooldowns(request)) return null;
        if(!(request.caster() instanceof ServerPlayer)) {
            return cooldowns(request).stream().filter(q->q.remaining()>0).max(Comparator.comparingLong(CooldownQuote::remaining))
                    .map(q->new CastRejection(null,q.remaining())).orElse(null);
        }
        var player=(ServerPlayer)request.caster();
        var cooldowns=MagicData.getPlayerMagicData(player).getPlayerCooldowns();
        dev.arsconflux.api.casting.CastRejection selected=null;
        for(var spell:spells(List.of(request.spell()))) if(cooldowns.isOnCooldown(spell)) {
            var record=cooldowns.getSpellCooldowns().get(spell.getSpellId());
            long ticks=record==null?-1:record.getCooldownRemaining();
            if(selected==null || ticks>selected.remainingTicks()) selected=dev.arsconflux.api.casting.CastRejection.cooldown(spell.getDisplayName(player),ticks);
        }
        return selected;
    }
    @Override public void released(ActiveCastRequest request, List<Spell> recipes) {
        if (!usesCooldowns(request)) return;
        if(!(request.caster() instanceof ServerPlayer)) {
            RecastCooldowns.occurrences(recipes,request.caster()).forEach((spell,shots)->MOB_COOLDOWNS.extendRaw(request.caster(),spell.getSpellId(),duration(request,spell,shots)));
            return;
        }
        var source = request.source() == ActiveCastSource.WEAPON ? CastSource.SWORD : CastSource.SPELLBOOK;
        RecastCooldowns.occurrences(recipes, request.caster()).forEach((spell, shots) ->
                RecastCooldowns.add((ServerPlayer)request.caster(), spell, source, shots));
    }
    private static int duration(ActiveCastRequest request,AbstractSpell spell,int shots) {
        int ticks=request.caster() instanceof ServerPlayer player
                ?io.redspace.ironsspellbooks.capabilities.magic.MagicManager.getEffectiveSpellCooldown(spell,player,
                        request.source()==ActiveCastSource.WEAPON?CastSource.SWORD:CastSource.SPELLBOOK)
                :io.redspace.ironsspellbooks.api.util.Utils.applyCooldownReduction(spell.getSpellCooldown(),request.caster());
        return CastingTiming.cooldownTicks(request.caster(),RecastCooldowns.perShot(Math.max(0,ticks),shots));
    }
    @Override public List<CooldownQuote> cooldowns(ActiveCastRequest request) {
        if(!usesCooldowns(request))return List.of();
        var result=new ArrayList<CooldownQuote>();
        RecastCooldowns.occurrences(List.of(request.spell()),request.caster()).forEach((spell,shots)->{
            long remaining;
            if(request.caster() instanceof ServerPlayer player){
                var record=MagicData.getPlayerMagicData(player).getPlayerCooldowns().getSpellCooldowns().get(spell.getSpellId());
                remaining=record==null?0:Math.max(0,record.getCooldownRemaining());
            }else remaining=MOB_COOLDOWNS.remaining(request.caster(),spell.getSpellId());
            result.add(new CooldownQuote(dev.ironsnouveau.platform.Locations.id(spell.getSpellId()),duration(request,spell,shots),remaining));
        });
        return List.copyOf(result);
    }
    @Override public void chantProgress(ActiveCastRequest request, UUID token, int duration, int remaining) {
        if (remaining == duration) MovementRestrictions.begin(request.caster(), token, duration);
        if (remaining == 0) MovementRestrictions.end(request.caster(), token);
        if (request.caster() instanceof ServerPlayer player && !player.hasDisconnected())
            dev.ironsnouveau.platform.ClientPackets.chant(player, new dev.ironsnouveau.network.ChantStatePayload(token, duration, remaining));
    }
}
