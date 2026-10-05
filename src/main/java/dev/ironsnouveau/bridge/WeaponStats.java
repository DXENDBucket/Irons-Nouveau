package dev.ironsnouveau.bridge;

import io.redspace.ironsspellbooks.api.util.Utils;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.enchantment.Enchantments;

final class WeaponStats {
    private WeaponStats() {}
    static float damage(LivingEntity caster) {
        return caster.getAttribute(Attributes.ATTACK_DAMAGE) == null ? 0 : Utils.getWeaponDamage(caster);
    }
    static float fireAspect(LivingEntity caster) {
        var enchants = caster.getWeaponItem().get(DataComponents.ENCHANTMENTS);
        return enchants == null ? 0 : Utils.getEnchantmentLevel(caster.level(), Enchantments.FIRE_ASPECT, enchants);
    }
}
