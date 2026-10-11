package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import io.redspace.ironsspellbooks.api.magic.MagicHelper;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/** A single-shot bridge shares the native burst cooldown across its shots. */
public final class RecastCooldowns {
    private RecastCooldowns() {}
    private record Release(ServerPlayer player, AbstractSpell spell, CastSource source, int shots) {}
    private static final ThreadLocal<Release> RELEASE = new ThreadLocal<>();

    public static int shots(AbstractSpell spell, int level, LivingEntity caster) {
        // Other recasts are recalls, alternate actions or selection stages, not repeated attacks.
        String id = spell.getSpellId();
        if (!id.equals("irons_spellbooks:flaming_barrage") && !id.equals("irons_spellbooks:eldritch_blast")) return 1;
        return Math.max(1, spell.getRecastCount(level, caster));
    }
    public static Map<AbstractSpell, Integer> occurrences(List<Spell> recipes, LivingEntity caster) {
        var result = new LinkedHashMap<AbstractSpell, Integer>();
        for (var recipe : recipes) {
            int action = 0;
            var parts = dev.arsconflux.api.glyph.ArsSpellAccess.parts(recipe);
            for (int i = 0; i < parts.size(); i++) {
                var part = parts.get(i);
                if (!(part instanceof AbstractAugment)) action = i;
                var id = BoundSpellSupport.ironSpell(part);
                if (id == null) continue;
                var spell = SpellRegistry.getSpell(id);
                if (spell == SpellRegistry.none()) continue;
                int level = SpellLevels.resolve(caster, id, recipe.getAugments(action, caster));
                // Shared native cooldown: retain the longest per-shot duration across occurrences.
                result.merge(spell, shots(spell, level, caster), Math::min);
            }
        }
        return result;
    }
    public static int perShot(int ticks, int shots) {
        if (ticks <= 0) return ticks;
        return (int)(((long)ticks + Math.max(1, shots) - 1) / Math.max(1, shots));
    }
    public static void add(ServerPlayer player, AbstractSpell spell, CastSource source, int shots) {
        var previous = RELEASE.get();
        RELEASE.set(new Release(player, spell, source, shots));
        try { MagicHelper.MAGIC_MANAGER.addCooldown(player, spell, source); }
        finally { if (previous == null) RELEASE.remove(); else RELEASE.set(previous); }
    }
    /** Called after native CDR/sword modifiers, before native cooldown events and synchronization. */
    public static int adjust(AbstractSpell spell, Player player, CastSource source, int nativeTicks) {
        var release = RELEASE.get();
        return release != null && release.player() == player && release.spell() == spell && release.source() == source
                ? perShot(nativeTicks, release.shots()) : nativeTicks;
    }
}
