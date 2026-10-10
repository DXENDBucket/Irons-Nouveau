package dev.ironsnouveau.platform;

import net.minecraft.world.entity.LivingEntity;

public final class Casters {
    private Casters() {}
    public static boolean isFake(LivingEntity entity) {
        return entity instanceof net.minecraftforge.common.util.FakePlayer;
    }
}
