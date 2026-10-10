package dev.ironsnouveau.bridge;

import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import dev.ironsnouveau.api.GlyphDefinition;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import dev.arsconflux.api.context.CastContext;

public record Resolution(ServerLevel world, CastContext context, AbstractSpell spell,
                         GlyphDefinition definition, SpellStats stats) {
    public Resolution(ServerLevel world, LivingEntity caster, AbstractSpell spell, GlyphDefinition definition, SpellStats stats,
                      net.minecraft.world.phys.Vec3 incomingDirection) {
        this(world, CastContext.detached(caster, null, null).withIncomingDirection(incomingDirection), spell, definition, stats);
    }
    public Resolution(ServerLevel world, LivingEntity caster, AbstractSpell spell, GlyphDefinition definition, SpellStats stats) {
        this(world, caster, spell, definition, stats, net.minecraft.world.phys.Vec3.ZERO);
    }
    public LivingEntity caster() { return context.caster(); }
    public LivingEntity executor() { return context.executor(); }
    public LivingEntity damageOwner() { return context.damageOwner(); }
    public net.minecraft.world.phys.Vec3 incomingDirection() { return context.incomingDirection(); }
    public Resolution withExecutor(LivingEntity executor) { return new Resolution(world, context.withExecutor(executor), spell, definition, stats); }
    public Resolution withTarget(net.minecraft.world.phys.HitResult target) { return new Resolution(world, context.withTarget(target), spell, definition, stats); }
    public int level() { return dev.ironsnouveau.casting.SpellLevels.resolve(caster(), definition.spellId(), stats.getAugments()); }
    public double power() { return Math.max(0, spell.getSpellPower(level(), caster())); }
    public double duration() { return AugmentScaling.duration(stats.getDurationMultiplier()); }
}
