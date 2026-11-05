package dev.ironsnouveau.compat.wind;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import dev.ironsnouveau.api.NativeCastAdapter;
import dev.ironsnouveau.casting.CastExecution;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.Set;
public final class WindBladeAdapter implements NativeCastAdapter {
    @Override public boolean supportsMethod(AbstractSpellPart part){return dev.arsconflux.api.projectile.CarrierRegistry.of(part)!=null;}
    @Override public Set<AbstractAugment> supportedAugments(){return Set.of(AugmentAmplify.INSTANCE,AugmentDampen.INSTANCE,
            AugmentAccelerate.INSTANCE,AugmentDecelerate.INSTANCE,AugmentSplit.INSTANCE);}
    @Override public CastExecution createExecution(ServerLevel world,LivingEntity caster){return new WindBladeExecution(world,caster);}
}
