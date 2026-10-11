package dev.ironsnouveau.casting;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.arsconflux.api.casting.*;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.world.entity.LivingEntity;
import java.util.function.Supplier;
/** Compatibility facade. Includes all registered providers in a mixed active recipe. */
public final class ActiveCooldowns {
    private ActiveCooldowns() {}
    private static ActiveCastSource source(CastSource source) { return source == CastSource.SWORD ? ActiveCastSource.WEAPON : ActiveCastSource.CASTER_TOOL; }
    public static boolean allowed(Spell spell, LivingEntity caster) { return ActiveCasts.allowed(spell, caster, ActiveCastSource.CASTER_TOOL); }
    public static <T> T execute(Spell spell, LivingEntity caster, CastSource source, Supplier<T> action, T denied) {
        return ActiveCasts.execute(spell, caster, source(source), action, denied);
    }
    public static boolean allowDispatch(SpellResolver resolver) { return ActiveCasts.allowDispatch(resolver); }
    public static void dispatched(SpellResolver resolver, boolean success) { ActiveCasts.dispatched(resolver, success); }
    public static void arrowSpawned(net.minecraft.world.entity.Entity entity, boolean success) { ActiveCasts.arrowSpawned(entity, success); }
}
