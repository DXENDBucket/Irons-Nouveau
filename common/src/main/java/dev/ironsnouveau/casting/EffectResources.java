package dev.ironsnouveau.casting;

import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.bridge.Resolution;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.context.CastContexts;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Leases survive chunk reloads. No forced chunks; children stop on expiry, owner loss or permission loss. */
public final class EffectResources {
    private static final String KEY = "irons_nouveau_lease";
    private static final Set<Entity> ACTIVE = Collections.newSetFromMap(new IdentityHashMap<>());
    private static final ThreadLocal<Origin> ORIGIN = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> PAID_CHILDREN = ThreadLocal.withInitial(() -> false);
    private static final Map<Entity, TriggerMana> ACCOUNTS = new IdentityHashMap<>();
    private static final Map<Entity, Long> PAID = new IdentityHashMap<>();
    private static final Set<Entity> PAYING = Collections.newSetFromMap(new IdentityHashMap<>());
    private record Origin(CastContext context, ResourceLocation glyph, ResourceLocation spell, int level, int ticks, TriggerMana mana) {
        LivingEntity owner() { return context.caster(); }
    }
    private EffectResources() {}
    public static boolean pulse(Entity entity, BooleanSupplier action) { return pulse(entity, 1, action); }
    public static boolean pulse(Entity entity, int period, BooleanSupplier action) {
        if (entity == null || !(entity.level() instanceof ServerLevel world) || !entity.getPersistentData().contains(KEY)) return action.getAsBoolean();
        if (entity.getPersistentData().getCompound(KEY).getBoolean("one_shot")) return action.getAsBoolean();
        // Ordinary summoned creatures pay for their creation, not every autonomous attack.
        if (entity instanceof net.minecraft.world.entity.Mob && !(entity instanceof io.redspace.ironsspellbooks.entity.spells.wisp.WispEntity)) return action.getAsBoolean();
        long round = entity.level().getGameTime() / period;
        if (PAYING.contains(entity)) return action.getAsBoolean();
        if (PAID.getOrDefault(entity, Long.MIN_VALUE) == round) {
            PAYING.add(entity); try { return action.getAsBoolean(); } finally { PAYING.remove(entity); }
        }
        var tag = entity.getPersistentData().getCompound(KEY);
        if (!tag.hasUUID("owner")) return false;
        Entity owner = world.getEntity(tag.getUUID("owner"));
        if (!(owner instanceof LivingEntity living) || !living.isAlive()) return false;
        var mana = ACCOUNTS.computeIfAbsent(entity, e -> TriggerMana.of(null, living));
        PAYING.add(entity);
        try {
            boolean applied = PresetTools.scoped(preset(entity, living), () ->
                    CastContexts.scoped(restored(entity, living, mana), () -> mana.trigger(dev.ironsnouveau.platform.Locations.id(tag.getString("spell")), tag.getInt("level"), action)));
            if (applied) PAID.put(entity, round);
            return applied;
        } finally { PAYING.remove(entity); }
    }
    public static int ticks(double ticks) { return (int)net.minecraft.util.Mth.clamp(ticks, 1, 96000); }
    public static boolean scoped(CastSession session, BooleanSupplier action) {
        return scoped(new Origin(session.context(), session.plan().glyphId(), session.plan().spellId(), session.plan().spellLevel(), 1200, session.billingSource()), action);
    }
    private static boolean scoped(Origin origin, BooleanSupplier action) {
        var previous = ORIGIN.get(); ORIGIN.set(origin);
        try { return CastContexts.scoped(origin.context(), action::getAsBoolean); }
        finally { if (previous == null) ORIGIN.remove(); else ORIGIN.set(previous); }
    }
    public static boolean spawn(Resolution ctx, Entity entity, int ticks) {
        return scoped(new Origin(ctx.context(), ctx.definition().glyphId(), ctx.definition().spellId(), ctx.level(), ticks, TriggerMana.of(ctx.context())),
                () -> ctx.world().hasChunkAt(entity.blockPosition()) && ctx.world().addFreshEntity(entity));
    }
    /** An already-paid instantaneous cast may resolve later; its first impact is not a new activation. */
    public static boolean spawnOnce(Resolution ctx, Entity entity, int ticks) {
        boolean spawned = spawn(ctx, entity, ticks);
        if (spawned) entity.getPersistentData().getCompound(KEY).putBoolean("one_shot", true);
        return spawned;
    }
    public static boolean managed(Entity entity) { return entity.getPersistentData().contains(KEY); }
    public static boolean paidChildren(Resolution ctx, int ticks, BooleanSupplier action) {
        boolean previous = PAID_CHILDREN.get(); PAID_CHILDREN.set(true);
        try { return scoped(new Origin(ctx.context(), ctx.definition().glyphId(), ctx.definition().spellId(), ctx.level(), ticks,
                TriggerMana.of(ctx.context())), action); }
        finally { PAID_CHILDREN.set(previous); }
    }
    public static boolean paidChildren(CastSession session, BooleanSupplier action) {
        boolean previous = PAID_CHILDREN.get(); PAID_CHILDREN.set(true);
        try { return scoped(session, action); } finally { PAID_CHILDREN.set(previous); }
    }
    /** An emitter pays per emission; its projectiles have already paid for their eventual impact. */
    public static void emit(Entity parent, int lifetime, boolean firstFree, Runnable action) {
        if (!(parent.level() instanceof ServerLevel world) || !managed(parent)) { action.run(); return; }
        var tag = parent.getPersistentData().getCompound(KEY);
        if (!tag.hasUUID("owner")) return;
        BooleanSupplier spawn = () -> {
            boolean previous = PAID_CHILDREN.get(); PAID_CHILDREN.set(true);
            try { children(parent, lifetime, action); return true; }
            finally { PAID_CHILDREN.set(previous); }
        };
        if (firstFree && !tag.getBoolean("emitted")) { tag.putBoolean("emitted", true); spawn.getAsBoolean(); }
        else {
            // Emission itself is billed even when the parent projectile's initial impact was prepaid.
            var owner = world.getEntity(tag.getUUID("owner"));
            if (owner instanceof LivingEntity living && living.isAlive())
                ACCOUNTS.computeIfAbsent(parent, e -> TriggerMana.of(null, living))
                        .trigger(dev.ironsnouveau.platform.Locations.id(tag.getString("spell")), tag.getInt("level"), spawn);
        }
    }
    /** Delayed native child creation must inherit the parent attribution and billing account. */
    public static void children(Entity parent, int duration, Runnable action) {
        var tag = parent.getPersistentData().getCompound(KEY);
        if (!(parent.level() instanceof ServerLevel world) || !tag.hasUUID("owner")) { action.run(); return; }
        if (!(world.getEntity(tag.getUUID("owner")) instanceof LivingEntity owner) || !owner.isAlive()) return;
        var spell = ResourceLocation.tryParse(tag.getString("spell"));
        var glyph = ResourceLocation.tryParse(tag.getString("glyph"));
        if (spell == null || glyph == null) return;
        PresetTools.scoped(preset(parent, owner), () -> scoped(new Origin(restored(parent, owner, ACCOUNTS.computeIfAbsent(parent, e -> TriggerMana.of(null, owner))), glyph, spell, tag.getInt("level"), ticks(duration),
                ACCOUNTS.get(parent)), () -> { action.run(); return true; }));
    }
    private static CastContext restored(Entity entity, LivingEntity owner, TriggerMana mana) {
        var tag = entity.getPersistentData().getCompound(KEY);
        var damage = tag.hasUUID("damage_owner") ? ((ServerLevel)entity.level()).getEntity(tag.getUUID("damage_owner")) : owner;
        return CastContext.detached(owner, new net.minecraft.world.phys.EntityHitResult(entity), mana.account())
                .withDamageOwner(damage instanceof LivingEntity living ? living : owner);
    }
    public static void stop() {
        for (var entity : List.copyOf(ACTIVE)) remove(entity);
        ACTIVE.clear();
    }
    /** Returns true only when the join must be cancelled by the loader adapter. */
    public static boolean joined(Entity entity, net.minecraft.world.level.Level level) {
        if (!(level instanceof ServerLevel world)) return false;
        var origin = ORIGIN.get();
        if (origin != null && (BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).getNamespace().equals("irons_spellbooks")
                || entity instanceof io.redspace.ironsspellbooks.entity.spells.ExtendedEvokerFang
                || entity instanceof io.redspace.ironsspellbooks.entity.spells.ExtendedFireworkRocket)
                && (!(entity instanceof NativeCastCarrier carrier) || carrier.ironsNouveau$session() == null)) {
            long owned = ACTIVE.stream().filter(e -> !e.isRemoved() && e.getPersistentData().getCompound(KEY).hasUUID("owner")
                    && e.getPersistentData().getCompound(KEY).getUUID("owner").equals(origin.owner().getUUID())).count();
            if (ACTIVE.size() >= 4096 || owned >= 256) { return true; }
            var tag = new CompoundTag();
            tag.putUUID("owner", origin.owner().getUUID());
            if (origin.context().damageOwner() != null) tag.putUUID("damage_owner", origin.context().damageOwner().getUUID());
            tag.putString("glyph", origin.glyph().toString());
            tag.putString("spell", origin.spell().toString()); tag.putInt("level", origin.level());
            tag.putLong("expires", world.getGameTime() + origin.ticks());
            var preset = PresetTools.current();
            if (preset != null && preset.caster() == origin.owner()
                    && PresetTools.level(origin.owner(), origin.spell()) > 0)
                tag.put("preset_tool", dev.ironsnouveau.platform.BoundSpellData.saveItem(preset.stack(), level.registryAccess()));
            if (PAID_CHILDREN.get()) tag.putBoolean("one_shot", true);
            entity.getPersistentData().put(KEY, tag);
        }
        if (entity.getPersistentData().contains(KEY)) {
            ACTIVE.add(entity);
            var account = origin == null ? TriggerMana.current() : origin.mana();
            if (account != null) ACCOUNTS.put(entity, account);
        }
        return false;
    }
    public static void tick() {
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
                        || !PresetTools.scoped(preset(entity, caster), () -> GlyphAccessEvent.allowed(caster, glyph, spell,
                                tag.getInt("level"), GlyphAccessEvent.Action.RESOLVE))) remove(entity);
            }
        }
    }
    private static void remove(Entity entity) {
        SummonManager.removeSummon(entity); entity.discard(); ACTIVE.remove(entity); ACCOUNTS.remove(entity); PAID.remove(entity);
    }
    private static PresetTools.Token preset(Entity entity, LivingEntity owner) {
        var tag = entity.getPersistentData().getCompound(KEY);
        if (!tag.contains("preset_tool")) return null;
        var stack = dev.ironsnouveau.platform.BoundSpellData.loadItem(tag.getCompound("preset_tool"), entity.level().registryAccess());
        return PresetTools.isPreset(stack) ? new PresetTools.Token(owner, stack) : null;
    }
}
