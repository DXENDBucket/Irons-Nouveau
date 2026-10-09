package dev.ironsnouveau.casting;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.entity.spells.fire_breath.FireBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.poison_breath.PoisonBreathProjectile;
import io.redspace.ironsspellbooks.entity.spells.cone_of_cold.ConeOfColdProjectile;
import io.redspace.ironsspellbooks.util.ParticleHelper;
import net.minecraft.core.particles.*;
import net.minecraft.world.phys.*;

/** Shared pose and particle geometry; no client-only class references on the dedicated server. */
public final class BreathVisuals {
    private BreathVisuals() {}
    public static void place(AbstractConeProjectile cone, Vec3 origin, Vec3 direction) {
        cone.setPos(origin); direction = direction.normalize();
        cone.setYRot((float)Math.toDegrees(Math.atan2(-direction.x, direction.z)));
        cone.setXRot((float)-Math.toDegrees(Math.atan2(direction.y, direction.horizontalDistance())));
        cone.yRotO = cone.getYRot(); cone.xRotO = cone.getXRot();
        float scale = ((ConeState)cone).ironsNouveau$scale(); var parts = cone.getParts();
        for (int i = 0; i < parts.length; i++) {
            var dimensions = parts[i].getDimensions(null);
            var pos = origin.add(direction.scale((1 + i * dimensions.width / 2) * scale));
            parts[i].setPos(pos);
            double half = dimensions.width * scale / 2, height = dimensions.height * scale;
            parts[i].setBoundingBox(new AABB(pos.x - half, pos.y, pos.z - half, pos.x + half, pos.y + height, pos.z + half));
        }
    }
    public static void tick(AbstractConeProjectile cone) {
        var state = (ConeState)cone; var anchor = cone.level().getEntity(state.ironsNouveau$anchor());
        if (anchor != null) place(cone, anchor.getEyePosition().add(state.ironsNouveau$offset()), anchor.getLookAngle());
        // Electrocute's subclass refreshes its native beam cache after this base tick.
        // It renders geometry, not breath particles; do not add the dragon fallback.
        if (cone instanceof io.redspace.ironsspellbooks.entity.spells.electrocute.ElectrocuteProjectile) return;
        var direction = cone.getLookAngle(); var pos = particleOrigin(cone);
        var random = cone.level().random;
        int count = cone instanceof PoisonBreathProjectile ? 20 : 12;
        for (int i = 0; i < count; i++) {
            ParticleOptions particle = cone instanceof FireBreathProjectile ? ParticleHelper.FIRE_EMITTER
                    : cone instanceof PoisonBreathProjectile ? (random.nextFloat() < .25 ? ParticleHelper.ACID_BUBBLE : ParticleHelper.ACID)
                    : cone instanceof ConeOfColdProjectile ? (random.nextFloat() < .15 ? ParticleHelper.SNOWFLAKE : ParticleHelper.SNOW_DUST)
                    : ParticleTypes.DRAGON_BREATH;
            var motion = direction.scale(3).add(random.nextGaussian() * .3, random.nextGaussian() * .3, random.nextGaussian() * .3)
                    .normalize().scale((.35 + random.nextDouble() * .35) * state.ironsNouveau$scale());
            cone.level().addParticle(particle, pos.x + random.nextGaussian() * .1, pos.y + random.nextGaussian() * .1,
                    pos.z + random.nextGaussian() * .1, motion.x, motion.y, motion.z);
        }
    }
    /** Match Iron's native mouth-height and forward offsets; block triggers retain their hit origin. */
    public static Vec3 particleOrigin(AbstractConeProjectile cone) {
        var state = (ConeState)cone;
        var anchor = cone.level().getEntity(state.ironsNouveau$anchor());
        if (anchor != null) return anchor.position().add(0, anchor.getEyeHeight() * .9f, 0)
                .add(anchor.getLookAngle().normalize().scale(cone instanceof ConeOfColdProjectile ? 1.5 : 1.6));
        return cone.position().add(cone.getLookAngle().scale(.35));
    }
}
