package dev.ironsnouveau.casting;

import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.network.CastingMovementPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Player-only leases belong to the chanter/breath host, independently of damage and mana ownership. */
public final class MovementRestrictions {
    private static final Map<UUID, State> ACTIVE = new HashMap<>();
    private static final class State {
        final ServerPlayer entity;
        final ServerLevel world;
        final Map<UUID, Long> leases = new HashMap<>();
        long lastSync;
        State(ServerPlayer entity, ServerLevel world) { this.entity = entity; this.world = world; }
        int remaining() {
            return (int)net.minecraft.util.Mth.clamp(leases.values().stream().mapToLong(Long::longValue).max().orElse(0) - world.getGameTime(), 0, Integer.MAX_VALUE);
        }
        void sync() {
            lastSync = world.getGameTime();
            if (!entity.hasDisconnected())
                dev.ironsnouveau.platform.ClientPackets.movement(entity, new CastingMovementPayload(entity.getId(), world.dimension().location(), remaining()));
        }
        void clear() {
            leases.clear(); sync();
        }
    }
    private MovementRestrictions() {}
    public static void stop() { List.copyOf(ACTIVE.values()).forEach(State::clear); ACTIVE.clear(); }
    public static void unload(net.minecraft.world.level.LevelAccessor level) {
        for (var state : List.copyOf(ACTIVE.values())) if (state.world == level) remove(state);
    }
    public static void begin(LivingEntity entity, UUID token, int ticks) {
        if (!(entity instanceof ServerPlayer player) || dev.ironsnouveau.platform.Casters.isFake(player)
                || !entity.isAlive() || !(entity.level() instanceof ServerLevel world) || ticks <= 0) return;
        var state = ACTIVE.get(entity.getUUID());
        if (state != null && (state.entity != entity || state.world != world)) { remove(state); state = null; }
        if (state == null) {
            if (ACTIVE.size() >= 4096) return;
            state = new State(player, world); ACTIVE.put(entity.getUUID(), state);
        }
        state.leases.put(token, world.getGameTime() + ticks);
        state.sync();
    }
    public static void end(LivingEntity entity, UUID token) {
        if (entity == null) return;
        var state = ACTIVE.get(entity.getUUID());
        if (state == null || state.entity != entity || state.leases.remove(token) == null) return;
        if (state.leases.isEmpty()) remove(state);
        else state.sync();
    }
    public static boolean active(LivingEntity entity) {
        var state = ACTIVE.get(entity.getUUID());
        return SpellLevelConfig.movementEnabled() && state != null && state.entity == entity && state.remaining() > 0;
    }
    private static void remove(State state) { ACTIVE.remove(state.entity.getUUID(), state); state.clear(); }
    public static void tick() {
        for (var state : List.copyOf(ACTIVE.values())) {
            if (!state.entity.isAlive() || state.entity.isRemoved() || state.entity.level() != state.world
                    || state.entity.hasDisconnected()) { remove(state); continue; }
            boolean changed = state.leases.values().removeIf(end -> end <= state.world.getGameTime());
            if (state.leases.isEmpty()) { remove(state); continue; }
            if (changed || state.world.getGameTime() - state.lastSync >= 20) state.sync();
        }
    }
}
