package dev.ironsnouveau.progression;

import com.mojang.serialization.Codec;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

/** Personal crafting history. A spell's best result never decreases. */
public record CraftedSpells(Map<ResourceLocation, Integer> levels) {
    public static final CraftedSpells EMPTY = new CraftedSpells(Map.of());
    public static final Codec<CraftedSpells> CODEC = Codec.unboundedMap(ResourceLocation.CODEC,
            Codec.intRange(1, Integer.MAX_VALUE)).xmap(CraftedSpells::new, CraftedSpells::levels);
    public net.minecraft.nbt.CompoundTag save() {
        var tag = new net.minecraft.nbt.CompoundTag();
        levels.forEach((id, level) -> tag.putInt(id.toString(), level));
        return tag;
    }
    public static CraftedSpells load(net.minecraft.nbt.CompoundTag tag) {
        var levels = new HashMap<ResourceLocation, Integer>();
        for (String key : tag.getAllKeys()) {
            var id = ResourceLocation.tryParse(key); int level = tag.getInt(key);
            if (id != null && level > 0) levels.put(id, level);
        }
        return new CraftedSpells(levels);
    }
    public CraftedSpells { levels = Map.copyOf(levels); }
    public int level(ResourceLocation spell) { return levels.getOrDefault(spell, 0); }
    public CraftedSpells record(ResourceLocation spell, int level) {
        if (level <= level(spell)) return this;
        var copy = new HashMap<>(levels); copy.put(spell, level); return new CraftedSpells(copy);
    }
}
