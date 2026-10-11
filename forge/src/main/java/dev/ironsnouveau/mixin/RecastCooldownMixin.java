package dev.ironsnouveau.mixin;

import dev.ironsnouveau.casting.RecastCooldowns;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = MagicManager.class, remap = false)
public abstract class RecastCooldownMixin {
    @Inject(method = "getEffectiveSpellCooldown", at = @At("RETURN"), cancellable = true)
    private static void ironsNouveau$perShot(AbstractSpell spell, Player player, CastSource source,
                                             CallbackInfoReturnable<Integer> result) {
        result.setReturnValue(RecastCooldowns.adjust(spell, player, source, result.getReturnValue()));
    }
}
