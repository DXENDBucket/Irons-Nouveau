package dev.ironsnouveau.casting;
import com.hollingsworth.arsnouveau.api.spell.*;
/** Legacy entry points delegate to Conflux's shared branch budget. */
public final class ProjectileVolley {
    private ProjectileVolley() {}
    public static void begin(SpellResolver resolver) { dev.arsconflux.api.projectile.VolleyBudget.begin(resolver); }
    public static int splits(SpellStats stats, int requested) { return dev.arsconflux.api.projectile.VolleyBudget.splits(stats, requested); }
    public static void finish(SpellResolver resolver) { dev.arsconflux.api.projectile.VolleyBudget.finish(resolver); }
}
