package dev.ironsnouveau.platform;

import net.minecraft.resources.ResourceLocation;

public final class Locations {
    private Locations() {}
    public static ResourceLocation id(String value) { return ResourceLocation.parse(value); }
    public static ResourceLocation id(String namespace, String path) { return ResourceLocation.fromNamespaceAndPath(namespace, path); }
}
