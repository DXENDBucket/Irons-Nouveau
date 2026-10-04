package dev.ironsnouveau.casting;
import com.hollingsworth.arsnouveau.api.spell.*;
import dev.ironsnouveau.glyph.NativeFormAugment;
import java.util.*;
/** Limit Ars' split count before its own fan generation; never change angles or projectile order. */
public final class ProjectileVolley {
    private record Batch(SpellResolver resolver, Map<SpellStats, Integer> counts) {}
    private static final ThreadLocal<Deque<Batch>> BATCHES = ThreadLocal.withInitial(ArrayDeque::new);
    private ProjectileVolley() {}
    public static void begin(SpellResolver resolver) { BATCHES.get().push(new Batch(resolver, new IdentityHashMap<>())); }
    public static int splits(SpellStats stats, int requested) {
        var stack = BATCHES.get();
        if (stack.isEmpty()) return requested;
        var batch = stack.peek(); var resolver = batch.resolver();
        var profile = CarrierProfiles.of(resolver.castType);
        if (profile == null || profile == CarrierProfiles.TRAIL || profile == CarrierProfiles.ORBIT) return requested;
        var form = stats.getAugments().stream().filter(NativeFormAugment.class::isInstance)
                .map(NativeFormAugment.class::cast).findFirst().orElse(null);
        if (form == null) return requested;
        return batch.counts().computeIfAbsent(stats, ignored -> {
            var context = resolver.spellContext; var caster = context.getUnwrappedCaster();
            int level = NativeCasting.level(form, NativeCasting.modifiers(resolver.spell, caster, 1), caster);
            return TriggerMana.of(context, caster).affordableCount(form.spellId(), level, requested + 1) - 1;
        });
    }
    public static void finish(SpellResolver resolver) {
        var stack = BATCHES.get();
        if (!stack.isEmpty() && stack.peek().resolver() == resolver) stack.pop();
        if (stack.isEmpty()) BATCHES.remove();
    }
}
