package dev.ironsnouveau.api;

import com.hollingsworth.arsnouveau.api.spell.SpellResolver;
import dev.ironsnouveau.bridge.Resolution;
import net.minecraft.world.phys.HitResult;

/** An adapter that may suspend the remaining Ars recipe. Ordinary location adapters stay unchanged. */
public interface InteractiveSpellAdapter extends LocationSpellAdapter {
    boolean begin(Resolution context, HitResult hit, SpellResolver resolver);
    @Override default boolean applyAt(Resolution context, HitResult hit) { return false; }
}
