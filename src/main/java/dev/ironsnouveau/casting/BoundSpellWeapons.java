package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.ironsnouveau.platform.BoundSpellData;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import java.util.Map;
import java.util.function.Supplier;

/** Platform facade preserving the existing NeoForge binding API. */
public final class BoundSpellWeapons extends BoundSpellSupport {
    public record Binding(Spell spell, Map<ResourceLocation, Integer> ironLevels) {
        public Binding { ironLevels = Map.copyOf(ironLevels); }
        public static final Codec<Binding> CODEC = RecordCodecBuilder.create(i -> i.group(
                Spell.CODEC.forGetter(Binding::spell),
                Codec.unboundedMap(ResourceLocation.CODEC, Codec.intRange(1, Integer.MAX_VALUE))
                        .optionalFieldOf("iron_levels", Map.of()).forGetter(Binding::ironLevels)
        ).apply(i, Binding::new));
    }
    public static final Supplier<DataComponentType<Binding>> BINDING = BoundSpellData.bindingType();
    private BoundSpellWeapons() {}
    public static void register(IEventBus bus) { BoundSpellData.register(bus); }
}
