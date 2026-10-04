package dev.ironsnouveau.casting;

import net.minecraft.resources.ResourceLocation;

/** Validated immutable effect parameters; no mutable Ars cursor or entity state belongs here. */
public record CastPlan(ResourceLocation glyphId, ResourceLocation spellId, int spellLevel,
                       float nativePower, CastModifiers modifiers, int maxTicks) {}
