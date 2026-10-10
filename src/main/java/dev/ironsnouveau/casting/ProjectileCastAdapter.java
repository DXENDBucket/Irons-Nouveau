package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import dev.ironsnouveau.api.NativeCastAdapter;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import java.util.Set;
import java.util.function.BiFunction;

public final class ProjectileCastAdapter implements NativeCastAdapter {
    private final BiFunction<Level, LivingEntity, AbstractMagicProjectile> factory;
    private final ProjectilePayload payload;
    public ProjectileCastAdapter(BiFunction<Level, LivingEntity, AbstractMagicProjectile> factory) {
        this(factory, ProjectilePayload.HALF_POWER);
    }
    public ProjectileCastAdapter(BiFunction<Level, LivingEntity, AbstractMagicProjectile> factory, ProjectilePayload payload) {
        this.factory = factory; this.payload = payload;
    }
    public ProjectilePayload payload() { return payload; }
    @Override public boolean supportsMethod(AbstractSpellPart method) { return dev.arsconflux.api.projectile.CarrierRegistry.of(method) != null; }
    @Override public Set<AbstractAugment> supportedAugments() {
        return Set.of(AugmentAmplify.INSTANCE, AugmentDampen.INSTANCE, AugmentAccelerate.INSTANCE,
                AugmentDecelerate.INSTANCE, AugmentSplit.INSTANCE);
    }
    @Override public CastExecution createExecution(ServerLevel world, LivingEntity caster) {
        return new ProjectileExecution(factory.apply(world, caster), payload);
    }
}
