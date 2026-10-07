package dev.ironsnouveau.casting;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.List;

public final class CastSessions {
    private static final Map<UUID, CastSession> ACTIVE = new LinkedHashMap<>();
    private CastSessions() {}
    public static boolean isCasting(net.minecraft.world.entity.LivingEntity caster) {
        return ACTIVE.values().stream().anyMatch(session -> session.active() && session.caster() == caster
                && (session.execution() instanceof BreathExecution || session.execution() instanceof SiphonRayExecution));
    }
    public static void register(IEventBus bus) {
        bus.addListener(CastSessions::tick);
        bus.addListener(CastSessions::stop);
        bus.addListener(CastSessions::unload);
    }
    public static boolean start(CastSession session) {
        if (ACTIVE.size() >= 4096 || !session.start()) {
            session.finish(CastSession.EndReason.SPAWN_FAILED);
            return false;
        }
        ACTIVE.put(session.id(), session);
        return true;
    }
    private static void tick(ServerTickEvent.Post event) {
        // Continuations can start new sessions; do not iterate a live mutable map.
        for (var session : List.copyOf(ACTIVE.values())) {
            session.tick();
            if (!session.active()) ACTIVE.remove(session.id());
        }
    }
    private static void unload(LevelEvent.Unload event) {
        for (var session : List.copyOf(ACTIVE.values())) if (session.world() == event.getLevel()) {
            session.finish(CastSession.EndReason.WORLD_UNLOADED);
            ACTIVE.remove(session.id());
        }
    }
    private static void stop(ServerStoppedEvent event) {
        List.copyOf(ACTIVE.values()).forEach(s -> s.finish(CastSession.EndReason.SERVER_STOPPED));
        ACTIVE.clear();
    }
}
