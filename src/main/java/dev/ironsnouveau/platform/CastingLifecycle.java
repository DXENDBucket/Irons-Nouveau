package dev.ironsnouveau.platform;

import dev.ironsnouveau.casting.ActiveChanting;
import dev.ironsnouveau.casting.MovementRestrictions;
import dev.ironsnouveau.casting.EffectResources;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

public final class CastingLifecycle {
    private CastingLifecycle() {}
    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent event) -> {
            if (!event.isCanceled() && EffectResources.joined(event.getEntity(), event.getLevel())) event.setCanceled(true);
        });
        bus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Pre event) -> EffectResources.tick());
        bus.addListener((net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) -> {
            ActiveChanting.tick(); MovementRestrictions.tick();
        });
        bus.addListener((ServerStoppingEvent event) -> EffectResources.stop());
        bus.addListener((ServerStoppedEvent event) -> { ActiveChanting.stop(); MovementRestrictions.stop(); });
        bus.addListener((LevelEvent.Unload event) -> {
            ActiveChanting.unload(event.getLevel()); MovementRestrictions.unload(event.getLevel());
        });
    }
}
