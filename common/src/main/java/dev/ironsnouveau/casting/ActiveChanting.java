package dev.ironsnouveau.casting;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.arsconflux.api.casting.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
/** Compatibility facade for explicit integrations. Scheduling and lifetime now belong to Conflux. */
public final class ActiveChanting {
    private ActiveChanting() {}
    public static boolean defer(Spell spell, LivingEntity caster, InteractionHand hand, Runnable release) {
        return ActiveCasts.defer(spell, caster, hand, ActiveCastSource.CASTER_TOOL, release) != ActiveCasts.Deferral.UNNEEDED;
    }
    public static boolean isChanting(LivingEntity caster) { return ActiveCasts.isChanting(caster); }
}
