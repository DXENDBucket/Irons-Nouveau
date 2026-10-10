package dev.ironsnouveau.platform;

import net.minecraft.resources.ResourceLocation;

public final class Locations {
    private Locations() {}
    public static ResourceLocation id(String value) { return new ResourceLocation(value); }
    public static ResourceLocation id(String namespace, String path) { return new ResourceLocation(namespace, path); }
}
