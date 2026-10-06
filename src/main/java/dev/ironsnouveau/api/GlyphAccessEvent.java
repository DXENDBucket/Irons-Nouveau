package dev.ironsnouveau.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.Cancelable;
import net.minecraftforge.common.MinecraftForge;

/** Integration hook after personal mastery checks; addons may impose additional permissions. */
@Cancelable
public final class GlyphAccessEvent extends Event {
    public enum Action { LEARN, RESOLVE }
    private final LivingEntity actor;
    private final ResourceLocation glyphId;
    private final ResourceLocation spellId;
    private final int spellLevel;
    private final Action action;

    public GlyphAccessEvent(LivingEntity actor, ResourceLocation glyphId, ResourceLocation spellId,
                            int spellLevel, Action action) {
        this.actor = actor;
        this.glyphId = glyphId;
        this.spellId = spellId;
        this.spellLevel = spellLevel;
        this.action = action;
    }
    public LivingEntity actor() { return actor; }
    public ResourceLocation glyphId() { return glyphId; }
    public ResourceLocation spellId() { return spellId; }
    public int spellLevel() { return spellLevel; }
    public Action action() { return action; }
    public static boolean allowed(LivingEntity actor, ResourceLocation glyph, ResourceLocation spell,
                                  int level, Action action) {
        if (!dev.ironsnouveau.progression.SpellProgress.canUse(actor, spell)) return false;
        return !MinecraftForge.EVENT_BUS.post(new GlyphAccessEvent(actor, glyph, spell, level, action));
    }
}
