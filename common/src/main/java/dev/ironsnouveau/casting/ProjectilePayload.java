package dev.ironsnouveau.casting;

import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.damage.DamageSources;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.BiConsumer;

/** Per-projectile damage and successful-hit semantics, independent of motion and cast scheduling. */
public class ProjectilePayload {
    private final float powerFactor;
    private final BiConsumer<CastSession, Entity> afterHit;
    public ProjectilePayload(float powerFactor, BiConsumer<CastSession, Entity> afterHit) { this.powerFactor = powerFactor; this.afterHit = afterHit; }
    public static final ProjectilePayload HALF_POWER = damage(.5f);
    public static final ProjectilePayload FULL_POWER = damage(1);
    public static final ProjectilePayload FLAMING_BARRAGE = new ProjectilePayload(1, (session, target) -> {}) {
        @Override public void configure(CastSession session, AbstractMagicProjectile projectile) {
            super.configure(session, projectile); projectile.setCursorHoming(true);
        }
        @Override public double launchSpeedMultiplier() { return .5; }
    };
    public static final ProjectilePayload BLOOD_NEEDLE = damage(.25f);
    public static final ProjectilePayload GUIDING_BOLT = new ProjectilePayload(.5f, (session, target) -> {
        if (target instanceof LivingEntity living)
            living.addEffect(dev.ironsnouveau.platform.Effects.create(MobEffectRegistry.GUIDING_BOLT, 500), session.caster());
    });
    public static ProjectilePayload damage(float factor) { return new ProjectilePayload(factor, (session, target) -> {}); }
    public float damage(CastPlan plan) { return plan.nativePower() * powerFactor; }
    public double launchSpeedMultiplier() { return 1; }
    public void configure(CastSession session, AbstractMagicProjectile projectile) { projectile.setDamage(damage(session.plan())); }
    public boolean apply(CastSession session, AbstractMagicProjectile projectile, Entity target) {
        if (target == projectile) return false;
        var spell = SpellRegistry.getSpell(session.plan().spellId());
        boolean success = DamageSources.applyDamage(target, projectile.getDamage(), spell.getDamageSource(projectile, session.damageOwner()));
        if (success) afterHit.accept(session, target);
        return success;
    }
}
