package dev.ironsnouveau.casting;
import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.bridge.Resolution;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.BooleanSupplier;
public final class StatusBilling {
    private static final String KEY = "irons_nouveau_thunderstorm";
    private static final Map<LivingEntity, TriggerMana> ACCOUNTS = new WeakHashMap<>();
    private StatusBilling() {}
    public static void mark(Resolution ctx, LivingEntity target, int duration) {
        var tag = new CompoundTag(); tag.putUUID("owner", ctx.caster().getUUID());
        tag.putLong("expires", ctx.world().getGameTime() + duration);
        tag.putInt("level", ctx.level());
        target.getPersistentData().put(KEY, tag);
        if (TriggerMana.current() != null) ACCOUNTS.put(target, TriggerMana.current());
    }
    public static boolean pulse(LivingEntity target, BooleanSupplier action) {
        if (!(target.level() instanceof ServerLevel world) || !target.getPersistentData().contains(KEY)) return action.getAsBoolean();
        var tag = target.getPersistentData().getCompound(KEY);
        if (world.getGameTime() >= tag.getLong("expires")) {
            target.getPersistentData().remove(KEY); ACCOUNTS.remove(target); return false;
        }
        if (!(world.getEntity(tag.getUUID("owner")) instanceof LivingEntity caster) || !caster.isAlive()) return false;
        int level = Math.max(1, tag.getInt("level")); // Pre-0.7 statuses were always level one.
        var spell = ResourceLocation.parse("irons_spellbooks:thunderstorm");
        if (!GlyphAccessEvent.allowed(caster, ResourceLocation.parse("irons_nouveau:glyph_thunderstorm"), spell, level, GlyphAccessEvent.Action.RESOLVE)) return false;
        ACCOUNTS.computeIfAbsent(target, e -> TriggerMana.of(null, caster)).trigger(spell, level, action);
        return true;
    }
}
