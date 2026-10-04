package dev.ironsnouveau.casting;

import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.bridge.Resolution;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Leases survive chunk reloads. No forced chunks; children stop on expiry, owner loss or permission loss. */
public final class EffectResources {
    private static final String KEY = "irons_nouveau_lease";
    private static final Set<Entity> ACTIVE = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final ThreadLocal<Origin> ORIGIN = new ThreadLocal<>();
    private static final Map<Entity, TriggerMana> ACCOUNTS = new IdentityHashMap<>();
    private static final Map<Entity, Long> PAID = new IdentityHashMap<>();
    private static final Set<Entity> PAYING = Collections.newSetFromMap(new IdentityHashMap<>());
    private record Origin(LivingEntity owner, ResourceLocation glyph, ResourceLocation spell, int level, int ticks, TriggerMana mana) {}
    private EffectResources() {}
    public static boolean pulse(Entity entity, BooleanSupplier action) { return pulse(entity, 1, action); }
    public static boolean pulse(Entity entity, int period, BooleanSupplier action) {
        if (entity == null || entity.level().isClientSide || !entity.getPersistentData().contains(KEY)) return action.getAsBoolean();
        if (entity.getPersistentData().getCompound(KEY).getBoolean("one_shot")) return action.getAsBoolean();
        // Ordinary summoned creatures pay for their creation, not every autonomous attack.
        if (entity instanceof net.minecraft.world.entity.Mob && !(entity instanceof io.redspace.ironsspellbooks.entity.spells.wisp.WispEntity)) return action.getAsBoolean();
        long round = entity.level().getGameTime() / period;
        if (PAYING.contains(entity)) return action.getAsBoolean();
        if (PAID.getOrDefault(entity, Long.MIN_VALUE) == round) {
            PAYING.add(entity); try { return action.getAsBoolean(); } finally { PAYING.remove(entity); }
        }
        var tag = entity.getPersistentData().getCompound(KEY);
        Entity owner = ((ServerLevel)entity.level()).getEntity(tag.getUUID("owner"));
        if (!(owner instanceof LivingEntity living) || !living.isAlive()) return false;
        var mana = ACCOUNTS.computeIfAbsent(entity, e -> TriggerMana.of(null, living));
        PAYING.add(entity);
        try {
            boolean applied = mana.trigger(ResourceLocation.parse(tag.getString("spell")), tag.getInt("level"), action);
            if (applied) PAID.put(entity, round);
            return applied;
        } finally { PAYING.remove(entity); }
    }
    public static int ticks(double ticks) { return (int)Math.clamp(ticks, 1, 96000); }
    public static boolean scoped(CastSession session, BooleanSupplier action) {
        return scoped(new Origin(session.caster(), session.plan().glyphId(), session.plan().spellId(), session.plan().spellLevel(), 1200, session.billingSource()), action);
    }
    private static boolean scoped(Origin origin, BooleanSupplier action) {
        var previous = ORIGIN.get(); ORIGIN.set(origin);
        try { return action.getAsBoolean(); }
        finally { if (previous == null) ORIGIN.remove(); else ORIGIN.set(previous); }
    }
    public static boolean spawn(Resolution ctx, Entity entity, int ticks) {
        return scoped(new Origin(ctx.caster(), ctx.definition().glyphId(), ctx.definition().spellId(), ctx.level(), ticks, TriggerMana.current()),
                () -> ctx.world().hasChunkAt(entity.blockPosition()) && ctx.world().addFreshEntity(entity));
    }
    /** An already-paid instantaneous cast may resolve later; its first impact is not a new activation. */
    public static boolean spawnOnce(Resolution ctx, Entity entity, int ticks) {
        boolean spawned = spawn(ctx, entity, ticks);
        if (spawned) entity.getPersistentData().getCompound(KEY).putBoolean("one_shot", true);
        return spawned;
    }
    /** Delayed native child creation must inherit the parent attribution and billing account. */
    public static void children(Entity parent, int duration, Runnable action) {
        var tag = parent.getPersistentData().getCompound(KEY);
        if (!(parent.level() instanceof ServerLevel world) || !tag.hasUUID("owner")) { action.run(); return; }
        if (!(world.getEntity(tag.getUUID("owner")) instanceof LivingEntity owner) || !owner.isAlive()) return;
        var spell = ResourceLocation.tryParse(tag.getString("spell"));
        var glyph = ResourceLocation.tryParse(tag.getString("glyph"));
        if (spell == null || glyph == null) return;
        scoped(new Origin(owner, glyph, spell, tag.getInt("level"), ticks(duration),
                ACCOUNTS.computeIfAbsent(parent, e -> TriggerMana.of(null, owner))), () -> { action.run(); return true; });
    }
    public static void register(IEventBus bus) {
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST, EffectResources::joined);
        bus.addListener(EffectResources::tick);
        bus.addListener((ServerStoppingEvent event) -> { for (var entity : List.copyOf(ACTIVE)) remove(entity); ACTIVE.clear(); });
    }
    private static void joined(EntityJoinLevelEvent event) {
        if (event.isCanceled() || !(event.getLevel() instanceof ServerLevel world)) return;
        Entity entity = event.getEntity(); var origin = ORIGIN.get();
        if (origin != null && (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("irons_spellbooks")
                || entity instanceof io.redspace.ironsspellbooks.entity.spells.ExtendedEvokerFang)
                && (!(entity instanceof NativeCastCarrier carrier) || carrier.ironsNouveau$session() == null)) {
            long owned = ACTIVE.stream().filter(e -> !e.isRemoved() && e.getPersistentData().getCompound(KEY).hasUUID("owner")
                    && e.getPersistentData().getCompound(KEY).getUUID("owner").equals(origin.owner().getUUID())).count();
            if (ACTIVE.size() >= 4096 || owned >= 256) { event.setCanceled(true); return; }
            var tag = new CompoundTag();
            tag.putUUID("owner", origin.owner().getUUID()); tag.putString("glyph", origin.glyph().toString());
            tag.putString("spell", origin.spell().toString()); tag.putInt("level", origin.level());
            tag.putLong("expires", world.getGameTime() + origin.ticks());
            entity.getPersistentData().put(KEY, tag);
        }
        if (entity.getPersistentData().contains(KEY)) {
            ACTIVE.add(entity);
            var account = origin == null ? TriggerMana.current() : origin.mana();
            if (account != null) ACCOUNTS.put(entity, account);
        }
    }
    private static void tick(ServerTickEvent.Pre event) {
        for (var entity : List.copyOf(ACTIVE)) {
            if (entity.isRemoved()) { ACTIVE.remove(entity); ACCOUNTS.remove(entity); PAID.remove(entity); continue; }
            var tag = entity.getPersistentData().getCompound(KEY);
            var world = (ServerLevel)entity.level();
            Entity owner = tag.hasUUID("owner") ? world.getEntity(tag.getUUID("owner")) : null;
            if (!(owner instanceof LivingEntity caster) || !caster.isAlive() || world.getGameTime() >= tag.getLong("expires")) {
                remove(entity); continue;
            }
            if (world.getGameTime() % 20 == 0) {
                var spell = ResourceLocation.tryParse(tag.getString("spell"));
                var glyph = ResourceLocation.tryParse(tag.getString("glyph"));
                if (spell == null || glyph == null || !SpellRegistry.getSpell(spell).isEnabled()
                        || !GlyphAccessEvent.allowed(caster, glyph, spell, tag.getInt("level"), GlyphAccessEvent.Action.RESOLVE)) remove(entity);
            }
        }
    }
    private static void remove(Entity entity) {
        SummonManager.removeSummon(entity); entity.discard(); ACTIVE.remove(entity); ACCOUNTS.remove(entity); PAID.remove(entity);
    }
}
