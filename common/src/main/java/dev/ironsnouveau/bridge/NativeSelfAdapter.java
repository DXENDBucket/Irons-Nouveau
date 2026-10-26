package dev.ironsnouveau.bridge;

import dev.ironsnouveau.api.LocationSpellAdapter;
import dev.ironsnouveau.casting.NativeSelfCasts;
import net.minecraft.world.phys.*;

/** Opaque native cast. Never redirect a remote hit back onto its owner. */
public final class NativeSelfAdapter implements LocationSpellAdapter {
    public static final NativeSelfAdapter INSTANCE=new NativeSelfAdapter();
    private NativeSelfAdapter() {}
    @Override public boolean applyAt(Resolution context,HitResult hit) {
        return hit instanceof EntityHitResult entity&&entity.getEntity()==context.caster()&&NativeSelfCasts.start(context);
    }
}
