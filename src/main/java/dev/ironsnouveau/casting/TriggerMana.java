package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import java.util.IdentityHashMap;
import java.util.function.BooleanSupplier;

/** Synchronous reservation prevents nested effects spending the same mana. Charge only committed triggers. */
public record TriggerMana(LivingEntity caster, IWrappedCaster source) {
    private static final ThreadLocal<IdentityHashMap<LivingEntity, Long>> RESERVED = ThreadLocal.withInitial(IdentityHashMap::new);
    private static final ThreadLocal<TriggerMana> CURRENT = new ThreadLocal<>();
    public static TriggerMana current() { return CURRENT.get(); }
    public static TriggerMana of(SpellContext context, LivingEntity caster) {
        return new TriggerMana(caster, context == null ? LivingCaster.from(caster) : context.getCaster());
    }
    public int affordableCount(ResourceLocation spell, int level, int requested) {
        if (caster instanceof Player player && player.isCreative()) return requested;
        int cost = Math.max(0, SpellRegistry.getSpell(spell).getManaCost(level));
        if (cost == 0) return requested;
        long held = RESERVED.get().getOrDefault(caster, 0L);
        int low = 0, high = requested;
        while (low < high) {
            int mid = low + (high - low + 1) / 2;
            long needed = held + (long) mid * cost;
            if (needed <= Integer.MAX_VALUE && source.enoughMana((int) needed)) low = mid; else high = mid - 1;
        }
        return low;
    }
    public boolean trigger(ResourceLocation spell, int level, BooleanSupplier action) {
        if (!caster.isAlive() || caster.level().isClientSide) return false;
        if (caster instanceof Player player && player.isCreative()) return run(action);
        int cost = Math.max(0, SpellRegistry.getSpell(spell).getManaCost(level));
        var reservations = RESERVED.get();
        long held = reservations.getOrDefault(caster, 0L), needed = held + cost;
        if (needed > Integer.MAX_VALUE || !source.enoughMana((int)needed)) return false;
        reservations.put(caster, needed);
        try {
            boolean applied = run(action);
            if (applied && cost > 0) source.expendMana(cost);
            return applied;
        } finally {
            if (held == 0) reservations.remove(caster); else reservations.put(caster, held);
            if (reservations.isEmpty()) RESERVED.remove();
        }
    }
    private boolean run(BooleanSupplier action) {
        var previous = CURRENT.get(); CURRENT.set(this);
        try { return action.getAsBoolean(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
}
