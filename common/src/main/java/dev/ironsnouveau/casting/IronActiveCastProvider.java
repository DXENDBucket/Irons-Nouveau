package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.casting.*;
import dev.ironsnouveau.config.SpellLevelConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
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
    public static void register() { ActiveCasts.register(dev.ironsnouveau.platform.Locations.id("irons_nouveau:active_casting"), new IronActiveCastProvider()); }
    @Override public boolean supports(Spell spell) { return ChantTiming.containsIron(spell); }
    @Override public boolean chantingEnabled(ActiveCastRequest request) { return SpellLevelConfig.chantingEnabled(); }
    @Override public List<Integer> chantTicks(ActiveCastRequest request) {
        var ticks = ChantTiming.occurrences(request.spell(), request.caster());
        // Preserve the existing Iron-only SUM setting as an explicit provider-group override.
        return SpellLevelConfig.chantMode() == SpellLevelConfig.ChantMode.SUM
                ? List.of(ChantDurations.combine(ticks, true)) : ticks;
    }
    private boolean cooldowns(ActiveCastRequest request) {
        return SpellLevelConfig.cooldownsEnabled() && request.caster() instanceof ServerPlayer player
                && !dev.ironsnouveau.platform.Casters.isFake(player)
                && (!player.isCreative() || ServerConfigs.CREATIVE_COOLDOWN.get());
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
    @Override public boolean allowed(ActiveCastRequest request) {
        if (!cooldowns(request)) return true;
        var player = (ServerPlayer)request.caster();
        var cooldowns = MagicData.getPlayerMagicData(player).getPlayerCooldowns();
        for (var spell : spells(List.of(request.spell()))) if (cooldowns.isOnCooldown(spell)) {
            player.displayClientMessage(Component.translatable("ui.irons_spellbooks.cast_error_cooldown",
                    spell.getDisplayName(player)).withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }
    @Override public void released(ActiveCastRequest request, List<Spell> recipes) {
        if (!cooldowns(request)) return;
        var source = request.source() == ActiveCastSource.WEAPON ? CastSource.SWORD : CastSource.SPELLBOOK;
        for (var spell : spells(recipes)) MagicHelper.MAGIC_MANAGER.addCooldown((ServerPlayer)request.caster(), spell, source);
    }
    @Override public void chantProgress(ActiveCastRequest request, UUID token, int duration, int remaining) {
        if (remaining == duration) MovementRestrictions.begin(request.caster(), token, duration);
        if (remaining == 0) MovementRestrictions.end(request.caster(), token);
        if (request.caster() instanceof ServerPlayer player && !player.hasDisconnected())
            dev.ironsnouveau.platform.ClientPackets.chant(player, new dev.ironsnouveau.network.ChantStatePayload(token, duration, remaining));
    }
}
