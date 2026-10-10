package dev.ironsnouveau.platform;
import io.redspace.ironsspellbooks.network.particles.FrostStepParticlesPacket;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
public final class StepParticles {
    private StepParticles() {}
    public static void frost(LivingEntity caster, Vec3 origin, Vec3 destination) {
        io.redspace.ironsspellbooks.setup.PacketDistributor.sendToPlayersTrackingEntityAndSelf(caster,
                new FrostStepParticlesPacket(origin, destination));
    }
}
