package dev.ironsnouveau.casting;

import dev.ironsnouveau.mixin.NativeProjectileHitAccess;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.fireball.MagicFireball;
import io.redspace.ironsspellbooks.entity.spells.fire_arrow.FireArrowProjectile;
import io.redspace.ironsspellbooks.entity.spells.poison_arrow.PoisonArrow;
import io.redspace.ironsspellbooks.entity.spells.magma_ball.FireBomb;
import io.redspace.ironsspellbooks.entity.spells.snowball.Snowball;
import io.redspace.ironsspellbooks.entity.spells.acid_orb.AcidOrb;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.spells.fire.*;
import io.redspace.ironsspellbooks.spells.nature.*;
import io.redspace.ironsspellbooks.spells.ice.SnowballSpell;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.HitResult;

/** Native impact callbacks retain explosions, statuses and child fields; a disposable impact entity leaves the Ars trajectory intact. */
public final class ComplexProjectilePayload extends ProjectilePayload {
    private final String id;
    public ComplexProjectilePayload(String id) { super(1, (s, e) -> {}); this.id = id; }
    public AbstractMagicProjectile create(Level level, LivingEntity caster) {
        return switch (id) {
            case "fireball" -> new MagicFireball(level, caster);
            case "fire_arrow" -> new FireArrowProjectile(level, caster);
            case "poison_arrow" -> new PoisonArrow(level, caster);
            case "magma_bomb" -> new FireBomb(level, caster);
            case "snowball" -> new Snowball(level, caster);
            case "acid_orb" -> new AcidOrb(level, caster);
            case "wither_skull" -> new io.redspace.ironsspellbooks.entity.spells.WitherSkullProjectile(level, caster);
            case "lob_creeper" -> new io.redspace.ironsspellbooks.entity.spells.creeper_head.CreeperHeadProjectile(level, caster);
            default -> throw new IllegalArgumentException(id);
        };
    }
    @Override public void configure(CastSession session, AbstractMagicProjectile p) {
        int level = session.plan().spellLevel(); var caster = session.caster();
        var spell = SpellRegistry.getSpell(session.plan().spellId());
        switch (id) {
            case "lob_creeper" -> { p.setDamage(session.plan().nativePower() * .5f); ((dev.ironsnouveau.mixin.CreeperHeadAccess)p).ironsNouveau$setSpeed((10 + level) * .08f); }
            case "wither_skull" -> { p.setDamage(session.plan().nativePower() * .5f); ((io.redspace.ironsspellbooks.entity.spells.WitherSkullProjectile)p).speed = (float)(6.0 + level) * .08f; }
            case "fireball" -> { var s = (FireballSpell)spell; p.setDamage(s.getDamage(level, caster)); p.setExplosionRadius(Math.min(48, s.getRadius(level, caster))); }
            case "fire_arrow" -> { var s = (FireArrowSpell)spell; p.setDamage(s.getDamage(level, caster)); p.setExplosionRadius(s.getRadius(level, caster)); }
            case "poison_arrow" -> { var s = (PoisonArrowSpell)spell; p.setDamage(s.getArrowDamage(level, caster)); ((PoisonArrow)p).setAoeDamage(s.getAOEDamage(level, caster)); }
            case "magma_bomb" -> { var s = (MagmaBombSpell)spell; p.setDamage(s.getDamage(level, caster)); p.setExplosionRadius(Math.min(48, s.getRadius(level, caster))); ((FireBomb)p).setAoeDamage(s.getAoeDamage(level, caster)); }
            case "snowball" -> { var s = (SnowballSpell)spell; p.setDamage(s.getDuration(level, caster)); p.setExplosionRadius(Math.min(48, s.getRadius(level, caster))); }
            case "acid_orb" -> { var s = (AcidOrbSpell)spell; p.setExplosionRadius(Math.min(48, s.getRadius(level, caster))); ((AcidOrb)p).setRendLevel(Math.clamp(s.getRendAmplifier(level, caster), 0, 255)); ((AcidOrb)p).setRendDuration(s.getRendDuration(level, caster)); }
        }
    }
    public boolean detonate(CastSession session, AbstractMagicProjectile visual, HitResult hit) {
        if (hit.getType() == HitResult.Type.MISS) return false;
        var impact = create(session.world(), session.caster()); configure(session, impact);
        impact.setOwner(session.damageOwner()); impact.setPos(hit.getLocation()); impact.setOldPosAndRot();
        impact.setDeltaMovement(visual.getDeltaMovement()); ((NativeCastCarrier)impact).ironsNouveau$session(session);
        if (!session.world().addFreshEntity(impact)) return false;
        try { ((NativeProjectileHitAccess)impact).ironsNouveau$hit(hit); return true; }
        finally { ((NativeCastCarrier)impact).ironsNouveau$session(null); impact.discard(); }
    }
}
