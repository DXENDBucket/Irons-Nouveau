package dev.ironsnouveau.casting;

import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.bridge.Resolution;
import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.context.CastContexts;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.BooleanSupplier;

/** A short native movement effect belongs to the casting player, not to its moving recipient. */
public final class MotionEffects {
    private static final String KEY = "irons_nouveau_motion";
    private static final ThreadLocal<Binding> CURRENT = new ThreadLocal<>();
    public record Binding(CastContext context, AbstractSpell spell, float damageScale) {
        public Binding(LivingEntity actor, LivingEntity owner, AbstractSpell spell, float damageScale) {
            this(CastContext.detached(owner, null, null).withExecutor(actor), spell, damageScale);
        }
        public LivingEntity actor() { return context.executor(); }
        public LivingEntity owner() { return context.damageOwner(); }
    }
    private MotionEffects() {}
    public static Binding current() { return CURRENT.get(); }
    public static void bind(Resolution ctx, LivingEntity actor, int ticks, float damage, int amplifier) {
        var tag = new CompoundTag();
        tag.putUUID("owner", ctx.caster().getUUID());
        tag.putUUID("damage_owner", ctx.damageOwner().getUUID());
        tag.putString("spell", ctx.spell().getSpellResource().toString());
        tag.putString("glyph", ctx.definition().glyphId().toString());
        tag.putInt("level", ctx.level()); tag.putFloat("scale", damage / amplifier);
        tag.putLong("expires", ctx.world().getGameTime() + ticks);
        actor.getPersistentData().put(KEY, tag);
    }
    public static boolean tick(LivingEntity actor, String spellPath, BooleanSupplier action) {
        if (!(actor.level() instanceof ServerLevel world)) return action.getAsBoolean();
        var tag = actor.getPersistentData().getCompound(KEY);
        if (!tag.getString("spell").equals("irons_spellbooks:" + spellPath)) return action.getAsBoolean();
        var spellId = ResourceLocation.tryParse(tag.getString("spell"));
        var glyphId = ResourceLocation.tryParse(tag.getString("glyph"));
        var entity = tag.hasUUID("owner") ? world.getEntity(tag.getUUID("owner")) : null;
        if (!(entity instanceof LivingEntity owner) || !owner.isAlive() || owner.isRemoved()
                || world.getGameTime() >= tag.getLong("expires") || spellId == null || glyphId == null
                || !SpellRegistry.getSpell(spellId).isEnabled()
                || !GlyphAccessEvent.allowed(owner, glyphId, spellId, tag.getInt("level"), GlyphAccessEvent.Action.RESOLVE)) {
            actor.getPersistentData().remove(KEY); return false;
        }
        var previous = CURRENT.get();
        var damageEntity = tag.hasUUID("damage_owner") ? world.getEntity(tag.getUUID("damage_owner")) : owner;
        if (!(damageEntity instanceof LivingEntity damageOwner) || !damageOwner.isAlive()) {
            actor.getPersistentData().remove(KEY); return false;
        }
        var frame = CastContext.detached(owner, new net.minecraft.world.phys.EntityHitResult(actor), null)
                .withExecutor(actor).withDamageOwner(damageOwner);
        CURRENT.set(new Binding(frame, SpellRegistry.getSpell(spellId), tag.getFloat("scale")));
        try {
            boolean keep = CastContexts.scoped(frame, action::getAsBoolean);
            if (!keep) actor.getPersistentData().remove(KEY);
            return keep;
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }
}
