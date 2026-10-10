package dev.ironsnouveau.casting;
import net.minecraftforge.eventbus.api.IEventBus;
import dev.arsconflux.api.execution.ExecutionSessions;
/** Conflux registers lifecycle events exactly once, including all participating addons. */
public final class CastSessions {
    private CastSessions() {}
    public static boolean isCasting(net.minecraft.world.entity.LivingEntity caster) { return ExecutionSessions.isCasting(caster); }
    @Deprecated public static void register(IEventBus bus) {}
    public static boolean start(CastSession session) { return ExecutionSessions.start(session.coreSession()); }
}
