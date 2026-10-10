package dev.ironsnouveau.glyph;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.api.GlyphDefinition;
import dev.ironsnouveau.bridge.*;
import dev.ironsnouveau.casting.TriggerMana;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.context.CastContexts;
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

public final class BridgeGlyph extends AbstractEffect implements dev.arsconflux.api.glyph.TriggerPaidGlyph {
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
        // Super calls virtual methods before definition is assigned. Populate translated hints here.
        for (var augment : compatibleAugments) {
            String key = augment == AugmentAOE.INSTANCE ? "area"
                    : augment == AugmentExtendTime.INSTANCE || augment == AugmentDurationDown.INSTANCE ? "duration" : "native_level";
            augmentDescriptions.put(augment, Component.translatable("irons_nouveau.augment." + key));
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
    @Override public void addAugmentDescriptions(Map<AbstractAugment, String> descriptions) {
        descriptions.put(AugmentAmplify.INSTANCE, "Adds one native spell level.");
        descriptions.put(AugmentDampen.INSTANCE, "Removes one native spell level, to a minimum of one.");
        descriptions.put(AugmentAOE.INSTANCE, "Affects nearby targets.");
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
        dev.ironsnouveau.casting.PresetTools.scoped(context, () -> {
            resolve(hit, world, caster, stats, context, resolver); return null;
        });
    }
    private void resolve(HitResult hit, Level world, LivingEntity caster, SpellStats stats,
                         SpellContext context, SpellResolver resolver) {
        if (!(world instanceof ServerLevel server) || !isEnabled()) return;
        var frame = CastContext.of(context, hit, null);
        if (frame.caster() == null || !frame.caster().isAlive()) return;
        var mana = TriggerMana.of(frame);
        frame = frame.withAccount(mana.account());
        CastContexts.bind(context, frame);
        var spell = SpellRegistry.getSpell(definition.spellId());
        var resolution = new Resolution(server, frame, spell, definition, stats);
        if (!permitted(frame.caster(), resolution.level(), GlyphAccessEvent.Action.RESOLVE)) return;
        boolean applied = CastContexts.scoped(frame, () -> mana.trigger(definition.spellId(), resolution.level(), () -> apply(resolution, hit)));
        if (applied) spell.getCastFinishSound().ifPresent(sound -> server.playSound(null,
                hit.getLocation().x, hit.getLocation().y, hit.getLocation().z, sound, SoundSource.PLAYERS, .7f, 1f));
    }
    private boolean apply(Resolution resolution, HitResult hit) {
        var server = resolution.world(); var caster = resolution.caster(); var stats = resolution.stats();
        if (definition.adapter() instanceof dev.ironsnouveau.api.LocationSpellAdapter location) return location.applyAt(resolution, hit);
        boolean applied = false;
        for (var target : Targeting.select(server, caster, hit, AugmentScaling.radius(stats.getAoeMultiplier()), definition.harmful())) {
            var selected = resolution.withTarget(new net.minecraft.world.phys.EntityHitResult(target));
            if (CastContexts.scoped(selected.context(), () -> definition.adapter().apply(selected, target))) {
                applied = true;
                server.sendParticles(definition.harmful() ? ParticleTypes.ENCHANT : ParticleTypes.HAPPY_VILLAGER,
                        target.getX(), target.getY() + target.getBbHeight() * .5, target.getZ(), 8, .25, .35, .25, .02);
            }
        }
        return applied;
    }
}
