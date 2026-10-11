package dev.ironsnouveau.platform;

import dev.ironsnouveau.casting.MovementRestrictions;
import dev.ironsnouveau.casting.EffectResources;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

public final class CastingLifecycle {
    private CastingLifecycle() {}
    public static void register(IEventBus bus) {
        bus.addListener(EventPriority.LOWEST, (EntityJoinLevelEvent event) -> {
            if (!event.isCanceled() && EffectResources.joined(event.getEntity(), event.getLevel())) event.setCanceled(true);
        });
        bus.addListener((net.minecraftforge.event.TickEvent.ServerTickEvent event) -> {
            if (event.phase == net.minecraftforge.event.TickEvent.Phase.START) EffectResources.tick();
            else { MovementRestrictions.tick(); }
        });
        bus.addListener((ServerStoppingEvent event) -> EffectResources.stop());
        bus.addListener((ServerStoppedEvent event) -> { MovementRestrictions.stop(); });
        bus.addListener((LevelEvent.Unload event) -> {
            MovementRestrictions.unload(event.getLevel());
        });
    }
}
