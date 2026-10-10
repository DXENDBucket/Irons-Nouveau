package dev.ironsnouveau.api;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import dev.ironsnouveau.casting.CastExecution;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.Set;

/** Form selection depends on execution capabilities, not on an entity/projectile superclass. */
public interface NativeCastAdapter {
    boolean supportsMethod(AbstractSpellPart method);
    Set<AbstractAugment> supportedAugments();
    CastExecution createExecution(ServerLevel world, LivingEntity caster);
}
