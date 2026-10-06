package dev.ironsnouveau.mixin;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.capability.ANPlayerDataCap;
import dev.ironsnouveau.progression.SpellProgress;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.Collection;

/** Enforce survival crafting history while leaving creative knowledge to Ars. */
@Mixin(value = ANPlayerDataCap.class, remap = false)
public abstract class IronGlyphKnowledgeMixin implements dev.ironsnouveau.progression.KnowledgeOwner {
    @org.spongepowered.asm.mixin.Unique private LivingEntity entity;
    @Override public void ironsNouveau$owner(LivingEntity owner) { entity = owner; }
    @Inject(method = "getKnownGlyphs", at = @At("RETURN"), cancellable = true)
    private void ironsNouveau$visible(CallbackInfoReturnable<Collection<AbstractSpellPart>> cir) {
        if (entity instanceof Player player) cir.setReturnValue(SpellProgress.visibleKnowledge(player, cir.getReturnValue()));
    }
    @Inject(method = "knowsGlyph", at = @At("HEAD"), cancellable = true)
    private void ironsNouveau$known(AbstractSpellPart part, CallbackInfoReturnable<Boolean> cir) {
        var spell = SpellProgress.spellId(part);
        if (dev.ironsnouveau.config.SpellLevelConfig.requiresCrafting() && spell != null && entity instanceof Player player && !player.isCreative())
            cir.setReturnValue(SpellProgress.canUse(player, spell));
    }
    @Inject(method = "unlockGlyph", at = @At("HEAD"), cancellable = true)
    private void ironsNouveau$earned(AbstractSpellPart part, CallbackInfoReturnable<Boolean> cir) {
        var spell = SpellProgress.spellId(part);
        if (spell != null && entity instanceof Player player && !SpellProgress.canUse(player, spell))
            cir.setReturnValue(false);
    }
}
