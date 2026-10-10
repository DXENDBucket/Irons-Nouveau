package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import com.hollingsworth.arsnouveau.common.entity.EntitySpellArrow;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.glyph.NativeFormAugment;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.config.ServerConfigs;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Supplier;

/** One authorization per explicit active use. Deferred hits never consult cooldowns again. */
public final class ActiveCooldowns {
    private static final ThreadLocal<Attempt> CURRENT = new ThreadLocal<>();
    private static final class Attempt {
        final ServerPlayer player;
        final Set<AbstractSpell> spells;
        final Set<AbstractSpell> deferred = new LinkedHashSet<>();
        final CastSource source;
        boolean released;
        Attempt(ServerPlayer player, Spell spell, CastSource source) { this.player = player; spells = spells(spell); this.source = source; }
    }
    private ActiveCooldowns() {}
    public static void arrowSpawned(net.minecraft.world.entity.Entity entity, boolean success) {
        if (success && CURRENT.get() != null && entity instanceof EntitySpellArrow arrow && dev.arsconflux.api.glyph.ArsSpellAccess.resolver(arrow) != null)
            dispatched(dev.arsconflux.api.glyph.ArsSpellAccess.resolver(arrow), true);
    }
    private static boolean applies(LivingEntity caster, Spell spell) {
        return SpellLevelConfig.cooldownsEnabled() && caster instanceof ServerPlayer player && !dev.ironsnouveau.platform.Casters.isFake(player)
                && (!player.isCreative() || ServerConfigs.CREATIVE_COOLDOWN.get()) && ChantTiming.containsIron(spell);
    }
    private static Set<AbstractSpell> spells(Spell spell) {
        var result = new LinkedHashSet<AbstractSpell>();
        for (var part : dev.arsconflux.api.glyph.ArsSpellAccess.parts(spell)) {
            var id = part instanceof BridgeGlyph glyph ? glyph.definition().spellId()
                    : part instanceof NativeFormAugment form ? form.spellId() : null;
            if (id != null) {
                var nativeSpell = SpellRegistry.getSpell(id);
                if (nativeSpell != SpellRegistry.none()) result.add(nativeSpell);
            }
        }
        return result;
    }
    public static boolean allowed(Spell spell, LivingEntity caster) {
        if (!applies(caster, spell)) return true;
        var player = (ServerPlayer)caster;
        var cooldowns = MagicData.getPlayerMagicData(player).getPlayerCooldowns();
        for (var nativeSpell : spells(spell)) if (cooldowns.isOnCooldown(nativeSpell)) {
            player.displayClientMessage(Component.translatable("ui.irons_spellbooks.cast_error_cooldown",
                    nativeSpell.getDisplayName(player)).withStyle(ChatFormatting.RED), true);
            return false;
        }
        return true;
    }
    /** The action must reach an acknowledged Ars dispatch/spawn to commit a cooldown. */
    public static <T> T execute(Spell spell, LivingEntity caster, CastSource source, Supplier<T> action, T denied) {
        if (!applies(caster, spell)) return action.get();
        if (!allowed(spell, caster)) return denied;
        var previous = CURRENT.get();
        if (previous != null && previous.player == caster) return action.get();
        var attempt = new Attempt((ServerPlayer)caster, spell, source);
        CURRENT.set(attempt);
        T result;
        try { result = action.get(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
        if (attempt.released && SpellLevelConfig.cooldownsEnabled()) {
            for (var nativeSpell : attempt.spells)
                if (!attempt.deferred.contains(nativeSpell)) MagicHelper.MAGIC_MANAGER.addCooldown(attempt.player, nativeSpell, source);
        }
        return result;
    }
    /** Check the resolved recipe too, in case the casting tool adds glyphs before dispatch. */
    public static boolean allowDispatch(SpellResolver resolver) {
        var attempt = CURRENT.get();
        return attempt == null || dev.arsconflux.api.glyph.ArsSpellAccess.caster(resolver.spellContext) != attempt.player
                || allowed(resolver.spell, attempt.player);
    }
    public static void dispatched(SpellResolver resolver, boolean success) {
        var attempt = CURRENT.get();
        if (!success || attempt == null || dev.arsconflux.api.glyph.ArsSpellAccess.caster(resolver.spellContext) != attempt.player
                || !ChantTiming.containsIron(resolver.spell)) return;
        attempt.spells.addAll(spells(resolver.spell));
        attempt.released = true;
    }
    /** Commit this spell's cooldown at session end, retaining the original active source/configuration. */
    public static Runnable deferCurrent(LivingEntity caster, AbstractSpell spell) {
        var attempt = CURRENT.get();
        if (attempt == null || attempt.player != caster) return () -> {};
        attempt.deferred.add(spell);
        return new Runnable() {
            private boolean committed;
            public void run() {
                if (committed) return;
                committed = true;
                if (SpellLevelConfig.cooldownsEnabled()) MagicHelper.MAGIC_MANAGER.addCooldown(attempt.player, spell, attempt.source);
            }
        };
    }
}
