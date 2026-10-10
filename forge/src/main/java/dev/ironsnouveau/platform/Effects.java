package dev.ironsnouveau.platform;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.Supplier;

/** Registry holders differ between Minecraft versions; effect rules remain shared. */
public final class Effects {
    private Effects() {}
    public static MobEffectInstance create(Supplier<MobEffect> effect, int ticks) {
        return create(effect, ticks, 0);
    }
    public static MobEffectInstance create(Supplier<MobEffect> effect, int ticks, int amplifier) {
        return create(effect, ticks, amplifier, false, true, true);
    }
    public static MobEffectInstance create(Supplier<MobEffect> effect, int ticks, int amplifier,
                                          boolean ambient, boolean visible, boolean icon) {
        return new MobEffectInstance(effect.get(), ticks, amplifier, ambient, visible, icon);
    }
    public static boolean has(LivingEntity entity, Supplier<MobEffect> effect) {
        return entity.hasEffect(effect.get());
    }
    public static boolean remove(LivingEntity entity, Supplier<MobEffect> effect) {
        return entity.removeEffect(effect.get());
    }
    public static boolean cleanseable(MobEffectInstance effect, net.minecraft.tags.TagKey<MobEffect> immune) {
        return effect.getEffect().getCategory() == net.minecraft.world.effect.MobEffectCategory.HARMFUL
                && !net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.wrapAsHolder(effect.getEffect()).is(immune);
    }
    public static void removeMagicEffects(LivingEntity entity) {
        for (var effect : java.util.List.copyOf(entity.getActiveEffectsMap().keySet()))
            if (effect instanceof io.redspace.ironsspellbooks.effect.MagicMobEffect)
                entity.removeEffect(effect);
    }
}
