package dev.ironsnouveau.bridge;

import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import dev.ironsnouveau.api.GlyphDefinition;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

public record Resolution(ServerLevel world, LivingEntity caster, AbstractSpell spell,
                         GlyphDefinition definition, SpellStats stats, net.minecraft.world.phys.Vec3 incomingDirection) {
    public Resolution(ServerLevel world, LivingEntity caster, AbstractSpell spell, GlyphDefinition definition, SpellStats stats) {
        this(world, caster, spell, definition, stats, net.minecraft.world.phys.Vec3.ZERO);
    }
    public int level() { return dev.ironsnouveau.casting.SpellLevels.resolve(caster, definition.spellId(), stats.getAugments()); }
    public double power() { return Math.max(0, spell.getSpellPower(level(), caster)); }
    public double duration() { return AugmentScaling.duration(stats.getDurationMultiplier()); }
}
