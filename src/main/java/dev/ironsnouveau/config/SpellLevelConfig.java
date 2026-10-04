package dev.ironsnouveau.config;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Server-owned policy, synchronized by NeoForge. Craft history remains intact across mode changes. */
public final class SpellLevelConfig {
    public enum Mode { PERSONAL_SCROLL, FIXED }
    public enum LearningMode { SCROLL_CRAFTING, ARS }
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Mode> MODE;
    public static final ModConfigSpec.IntValue DEFAULT_LEVEL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SPELL_LEVELS;
    public static final ModConfigSpec.BooleanValue CREATIVE_NATIVE_MAX;
    public static final ModConfigSpec.EnumValue<LearningMode> LEARNING_MODE;
    private static List<? extends String> cachedEntries = List.of();
    private static Map<ResourceLocation, Integer> cachedLevels = Map.of();

    static {
        var builder = new ModConfigSpec.Builder();
        builder.comment("Iron spell base levels used by Ars glyphs. Amplify/Dampen apply after this policy.").push("spell_levels");
        MODE = builder.comment("PERSONAL_SCROLL: highest native scroll personally crafted/upgraded (existing behavior).",
                "FIXED: configured default level, with optional overrides for individual Iron spell IDs.")
                .defineEnum("mode", Mode.PERSONAL_SCROLL);
        DEFAULT_LEVEL = builder.comment("Base level for spells without an override in FIXED mode. Not limited to the native scroll maximum.")
                .defineInRange("default_level", 1, 1, Integer.MAX_VALUE);
        SPELL_LEVELS = builder.comment("Used only in FIXED mode. Format: namespace:spell_id=positive_integer.",
                "Example: [\"irons_spellbooks:firebolt=3\", \"irons_spellbooks:heal=2\"]. Last duplicate wins.",
                "Use Iron spell IDs, not irons_nouveau:glyph_* IDs. Unknown but well-formed IDs are harmless.")
                .defineListAllowEmpty("overrides", List::of, () -> "irons_spellbooks:firebolt=1", SpellLevelConfig::validEntry);
        CREATIVE_NATIVE_MAX = builder.comment("Keep the existing creative-mode native maximum. Set false to use the selected level policy in creative too.")
                .define("creative_native_max", true);
        builder.pop();
        builder.push("learning");
        LEARNING_MODE = builder.comment("Independent of spell_levels.mode.",
                "SCROLL_CRAFTING: personally craft/upgrade a native Iron scroll to unlock its glyph (existing behavior).",
                "ARS: use Ars learned-glyph records and glyph items. Scribe recipes consume a matching Iron scroll and a source gem.",
                "ARS permits Iron glyphs in normal Ars random loot. Neither mode auto-teaches every glyph.")
                .defineEnum("mode", LearningMode.SCROLL_CRAFTING);
        builder.pop();
        SPEC = builder.build();
    }
    private SpellLevelConfig() {}
    private static <T> T read(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
    public static boolean requiresCrafting() { return read(LEARNING_MODE) == LearningMode.SCROLL_CRAFTING; }
    public static Mode mode() { return read(MODE); }
    public static boolean creativeNativeMax() { return read(CREATIVE_NATIVE_MAX); }
    public static int configuredLevel(ResourceLocation spell) {
        var entries = read(SPELL_LEVELS);
        // NeoForge config values are cached; rebuild only when the synchronized list changes.
        synchronized (SpellLevelConfig.class) {
            if (!entries.equals(cachedEntries)) {
                var levels = new HashMap<ResourceLocation, Integer>();
                for (var entry : entries) if (validEntry(entry)) {
                    int separator = entry.lastIndexOf('=');
                    levels.put(ResourceLocation.parse(entry.substring(0, separator).trim()),
                            Integer.parseInt(entry.substring(separator + 1).trim()));
                }
                cachedEntries = List.copyOf(entries);
                cachedLevels = Map.copyOf(levels);
            }
            return cachedLevels.getOrDefault(spell, read(DEFAULT_LEVEL));
        }
    }
    public static boolean validEntry(Object value) {
        if (!(value instanceof String entry)) return false;
        int separator = entry.lastIndexOf('=');
        if (separator <= 0) return false;
        String id = entry.substring(0, separator).trim();
        if (!id.startsWith("irons_spellbooks:") || ResourceLocation.tryParse(id) == null) return false;
        try { return Integer.parseInt(entry.substring(separator + 1).trim()) > 0; }
        catch (NumberFormatException ignored) { return false; }
    }
}
