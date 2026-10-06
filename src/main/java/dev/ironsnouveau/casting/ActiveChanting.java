package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.network.ChantStatePayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.event.TickEvent.ServerTickEvent;
import net.minecraftforge.network.PacketDistributor;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Explicit active-use boundary. Resolvers, hit effects and automatic casts are never intercepted globally. */
public final class ActiveChanting {
    private static final Map<UUID, Pending> ACTIVE = new HashMap<>();
    private record Pending(LivingEntity caster, ServerLevel world, InteractionHand hand,
                           ItemStack held, ItemStack snapshot, UUID token, long start, int ticks, Runnable release) {
        boolean valid() {
            return caster.isAlive() && !caster.isRemoved() && caster.level() == world
                    && (!(caster instanceof ServerPlayer player) || !player.hasDisconnected() && !player.isSpectator())
                    && caster.getItemInHand(hand) == held && ItemStack.matches(held, snapshot);
        }
        void sync(int remaining) {
            if (caster instanceof ServerPlayer player && !player.hasDisconnected())
                dev.ironsnouveau.network.ForgeNetwork.sendToPlayer(player, new ChantStatePayload(token, ticks, remaining));
        }
        void progress(long now) {
            sync((int)Math.max(0, ticks - (now - start)));
        }
    }
    private ActiveChanting() {}
    public static void register(IEventBus bus) {
        bus.addListener(ActiveChanting::tick);
        bus.addListener((ServerStoppedEvent e) -> {
            ACTIVE.values().forEach(p -> MovementRestrictions.end(p.caster, p.token));
            ACTIVE.clear();
        });
        bus.addListener((LevelEvent.Unload e) -> ACTIVE.values().removeIf(p -> {
            if (p.world != e.getLevel()) return false;
            p.sync(0);
            MovementRestrictions.end(p.caster, p.token);
            return true;
        }));
    }
    /** Returns true when this active action was queued (or an existing chant is still in progress). */
    public static boolean defer(Spell spell, LivingEntity caster, InteractionHand hand, Runnable release) {
        // This guard precedes every queue/state operation: pure Ars spells are completely unaffected.
        if (!SpellLevelConfig.chantingEnabled() || !ChantTiming.containsIron(spell)
                || !(caster.level() instanceof ServerLevel world)) return false;
        var previous = ACTIVE.get(caster.getUUID());
        if (previous != null) {
            if (previous.valid()) return true; // Holding right-click must not restart the countdown.
            ACTIVE.remove(caster.getUUID());
            previous.sync(0);
            MovementRestrictions.end(previous.caster, previous.token);
        }
        int ticks = ChantTiming.ticks(spell, caster, SpellLevelConfig.chantMode());
        if (ticks == 0 || !caster.isAlive() || ACTIVE.size() >= 4096) return false;
        var held = caster.getItemInHand(hand);
        var pending = new Pending(caster, world, hand, held, held.copy(), UUID.randomUUID(), world.getGameTime(), ticks, release);
        ACTIVE.put(caster.getUUID(), pending);
        MovementRestrictions.begin(caster, pending.token, ticks);
        pending.progress(world.getGameTime());
        return true;
    }
    public static boolean isChanting(LivingEntity caster) { return ACTIVE.containsKey(caster.getUUID()); }
    private static void tick(ServerTickEvent event) {
        if (event.phase != net.minecraftforge.event.TickEvent.Phase.END) return;
        for (var pending : List.copyOf(ACTIVE.values())) {
            if (!pending.valid() || !SpellLevelConfig.chantingEnabled()) {
                ACTIVE.remove(pending.caster.getUUID());
                pending.sync(0);
                MovementRestrictions.end(pending.caster, pending.token);
                continue;
            }
            long now = pending.world.getGameTime();
            if (now - pending.start >= pending.ticks) {
                ACTIVE.remove(pending.caster.getUUID());
                pending.sync(0);
                MovementRestrictions.end(pending.caster, pending.token);
                // Invoke the original active action: targeting, validation and payment occur on release.
                pending.release.run();
            } else if ((now - pending.start) % 20 == 0) pending.progress(now);
        }
    }
}
