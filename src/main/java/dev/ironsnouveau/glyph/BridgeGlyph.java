package dev.ironsnouveau.glyph;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.api.GlyphDefinition;
import dev.ironsnouveau.bridge.*;
import dev.ironsnouveau.casting.TriggerMana;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;
import java.util.Map;
import java.util.Set;

public final class BridgeGlyph extends AbstractEffect {
    private final GlyphDefinition definition;
    public BridgeGlyph(GlyphDefinition definition) {
        super(definition.glyphId(), definition.name());
        this.definition = definition;
        spellSchools.add(definition.school());
        definition.school().addSpellPart(this);
        if (definition.supportsDuration()) {
            compatibleAugments.add(AugmentExtendTime.INSTANCE);
            compatibleAugments.add(AugmentDurationDown.INSTANCE);
        }
    }
    public GlyphDefinition definition() { return definition; }
    @Override public int getDefaultManaCost() { return 0; }
    @Override public int getCastingCost() { return 0; }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    // Mastery belongs to Iron scroll crafting; old Ars tier configs must not add another gate.
    @Override public SpellTier getConfigTier() { return SpellTier.ONE; }
    @Override public boolean shouldShowInUnlock() { return !dev.ironsnouveau.config.SpellLevelConfig.requiresCrafting(); }
    @Override protected Set<AbstractAugment> getCompatibleAugments() {
        return Set.of(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE, AugmentAOE.INSTANCE);
    }
    @Override public Glyph getGlyph() {
        if (glyphItem == null) glyphItem = new Glyph(this) {
            @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
                if (!level.isClientSide && !permitted(player, dev.ironsnouveau.casting.SpellLevels.DEFAULT, GlyphAccessEvent.Action.LEARN))
                    return InteractionResultHolder.fail(player.getItemInHand(hand));
                return super.use(level, player, hand);
            }
        };
        return glyphItem;
    }
    public boolean permitted(LivingEntity caster, int level, GlyphAccessEvent.Action action) {
        var spell = SpellRegistry.getSpell(definition.spellId());
        return spell != SpellRegistry.none() && spell.isEnabled()
                && GlyphAccessEvent.allowed(caster, definition.glyphId(), definition.spellId(), level, action);
    }
    @Override public void onResolve(HitResult hit, Level world, LivingEntity caster, SpellStats stats,
                                    SpellContext context, SpellResolver resolver) {
        if (!(world instanceof ServerLevel server) || !isEnabled() || !caster.isAlive()) return;
        var spell = SpellRegistry.getSpell(definition.spellId());
        var resolution = new Resolution(server, caster, spell, definition, stats, dev.ironsnouveau.casting.TriggerGeometry.read(context));
        if (!permitted(caster, resolution.level(), GlyphAccessEvent.Action.RESOLVE)) return;
        boolean applied = TriggerMana.of(context, caster).trigger(definition.spellId(), resolution.level(), () -> apply(resolution, hit));
        if (applied) spell.getCastFinishSound().ifPresent(sound -> server.playSound(null,
                hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, sound, SoundSource.PLAYERS, .7f, 1f));
    }
    private boolean apply(Resolution resolution, HitResult hit) {
        var server = resolution.world(); var caster = resolution.caster(); var stats = resolution.stats();
        if (definition.adapter() instanceof dev.ironsnouveau.api.LocationSpellAdapter location) return location.applyAt(resolution, hit);
        boolean applied = false;
        for (var target : Targeting.select(server, caster, hit, AugmentScaling.radius(stats.getAoeMultiplier()), definition.harmful())) {
            if (definition.adapter().apply(resolution, target)) {
                applied = true;
                server.sendParticles(definition.harmful() ? ParticleTypes.ENCHANT : ParticleTypes.HAPPY_VILLAGER,
                        target.getX(), target.getY() + target.getBbHeight() * .5, target.getZ(), 8, .25, .35, .25, .02);
            }
        }
        return applied;
    }
}
