package dev.ironsnouveau.casting;

import net.minecraft.world.phys.Vec3;
import java.util.UUID;

/** Spatial input independent of delivery: usable by projectiles, targeted and sustained adapters. */
public record CastAim(Vec3 origin, Vec3 direction, UUID targetId) {}
