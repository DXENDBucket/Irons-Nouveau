package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.glyph.NativeFormAugment;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/** Recipe occurrences, not runtime branches. No access to Iron's native casting state. */
public final class ChantTiming {
    private ChantTiming() {}
    public static boolean containsIron(Spell spell) {
        return spell.recipe.stream().anyMatch(p -> p instanceof BridgeGlyph || p instanceof NativeFormAugment);
    }
    public static int ticks(Spell recipe, LivingEntity caster, SpellLevelConfig.ChantMode mode) {
        long total = 0;
        int action = 0;
        for (int i = 0; i < recipe.recipe.size(); i++) {
            var part = recipe.recipe.get(i);
            if (!(part instanceof AbstractAugment)) action = i;
            ResourceLocation id = part instanceof BridgeGlyph glyph ? glyph.definition().spellId()
                    : part instanceof NativeFormAugment form ? form.spellId() : null;
            if (id == null) continue;
            var nativeSpell = SpellRegistry.getSpell(id);
            // Channel length is already managed by the bridge; it is not a wind-up.
            if (nativeSpell == SpellRegistry.none() || nativeSpell.getCastType() != CastType.LONG) continue;
            int level = SpellLevels.resolve(caster, id, recipe.getAugments(action, caster));
            int ticks = Math.max(0, nativeSpell.getEffectiveCastTime(level, caster));
            total = mode == SpellLevelConfig.ChantMode.MAXIMUM ? Math.max(total, ticks) : total + ticks;
            total = Math.min(Integer.MAX_VALUE, total);
        }
        return (int)total;
    }
}
