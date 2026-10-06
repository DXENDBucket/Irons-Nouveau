package dev.ironsnouveau.bridge;

import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantments;

final class WeaponStats {
    private WeaponStats() {}
    static float damage(LivingEntity caster) {
        return caster.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 0 : Utils.getWeaponDamage(caster);
    }
    static float fireAspect(LivingEntity caster) {
        return EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_ASPECT, caster.getMainHandItem());
    }
}
