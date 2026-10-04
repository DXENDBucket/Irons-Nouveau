package dev.ironsnouveau.progression;

import com.mojang.serialization.Codec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

/** Personal crafting history. A spell's best result never decreases. */
public record CraftedSpells(Map<ResourceLocation, Integer> levels) {
    public static final CraftedSpells EMPTY = new CraftedSpells(Map.of());
    public static final Codec<CraftedSpells> CODEC = Codec.unboundedMap(ResourceLocation.CODEC,
            Codec.intRange(1, Integer.MAX_VALUE)).xmap(CraftedSpells::new, CraftedSpells::levels);
    public static final StreamCodec<RegistryFriendlyByteBuf, CraftedSpells> STREAM_CODEC = StreamCodec.of((buf, data) -> {
        buf.writeVarInt(data.levels.size());
        data.levels.forEach((id, level) -> { buf.writeResourceLocation(id); buf.writeVarInt(level); });
    }, buf -> {
        int size = buf.readVarInt();
        if (size < 0 || size > buf.readableBytes()) throw new IllegalArgumentException("Invalid crafting history size");
        var levels = new HashMap<ResourceLocation, Integer>();
        for (int i = 0; i < size; i++) {
            var id = buf.readResourceLocation(); int level = buf.readVarInt();
            if (level < 1 || levels.putIfAbsent(id, level) != null) throw new IllegalArgumentException("Invalid crafted spell");
        }
        return new CraftedSpells(levels);
    });
    public CraftedSpells { levels = Map.copyOf(levels); }
    public int level(ResourceLocation spell) { return levels.getOrDefault(spell, 0); }
    public CraftedSpells record(ResourceLocation spell, int level) {
        if (level <= level(spell)) return this;
        var copy = new HashMap<>(levels); copy.put(spell, level); return new CraftedSpells(copy);
    }
}
