package dev.ironsnouveau.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;

/** Optional non-player mastery source. Augments are applied after this base is resolved. */
public final class NativeSpellLevelEvent extends Event {
    private final LivingEntity caster;
    private final ResourceLocation spell;
    private int level;
    public NativeSpellLevelEvent(LivingEntity caster, ResourceLocation spell, int level) {
        this.caster = caster; this.spell = spell; this.level = level;
    }
    public LivingEntity caster() { return caster; }
    public ResourceLocation spell() { return spell; }
    public int level() { return level; }
    public void setLevel(int level) { this.level = Math.max(1, level); }
}
