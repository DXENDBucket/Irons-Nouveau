package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import dev.ironsnouveau.platform.BoundSpellData;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.eventbus.api.IEventBus;
import java.util.Map;

/** Forge facade; persistence uses item NBT rather than NeoForge components. */
public final class BoundSpellWeapons extends BoundSpellSupport {
    public record Binding(Spell spell, Map<ResourceLocation, Integer> ironLevels) {
        public Binding { ironLevels = Map.copyOf(ironLevels); }
    }
    private BoundSpellWeapons() {}
    public static void register(IEventBus bus) { BoundSpellData.register(bus); }
}
