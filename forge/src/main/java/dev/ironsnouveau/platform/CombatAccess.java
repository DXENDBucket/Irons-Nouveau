package dev.ironsnouveau.platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class CombatAccess {
    private CombatAccess() {}
    public static void postAttack(ServerLevel world, LivingEntity caster, Entity target, DamageSource source) {
        EnchantmentHelper.doPostDamageEffects(caster, target);
    }
    public static boolean noBlockCollision(ServerLevel world, Entity entity, AABB box) {
        return !world.getBlockCollisions(entity, box).iterator().hasNext();
    }
}
