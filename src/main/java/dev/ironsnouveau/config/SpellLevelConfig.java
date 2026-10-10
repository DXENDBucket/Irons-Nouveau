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
    public enum ChantMode { MAXIMUM, SUM }
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.EnumValue<Mode> MODE;
    public static final ModConfigSpec.IntValue DEFAULT_LEVEL;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> SPELL_LEVELS;
    public static final ModConfigSpec.BooleanValue CREATIVE_NATIVE_MAX;
    public static final ModConfigSpec.EnumValue<LearningMode> LEARNING_MODE;
    public static final ModConfigSpec.BooleanValue CHANTING_ENABLED;
    public static final ModConfigSpec.EnumValue<ChantMode> CHANTING_MODE;
    public static final ModConfigSpec.BooleanValue COOLDOWNS_ENABLED;
    public static final ModConfigSpec.BooleanValue MOVEMENT_ENABLED;
    public static final ModConfigSpec.BooleanValue USE_IRON_MANA;
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
        builder.push("chanting");
        CHANTING_ENABLED = builder.comment("Add a pre-cast chant to active Ars casts containing Iron & Nouveau glyphs.",
                "False restores immediate casting. Pure Ars/addon spells and subsequent hit/passive triggers are unchanged.")
                .define("enabled", true);
        CHANTING_MODE = builder.comment("MAXIMUM: longest Iron chant in the recipe. SUM: add each Iron glyph occurrence.",
                "Both use native Iron effective cast times, including cast-time reduction and the resolved spell level.",
                "Instant spells and continuous channel durations contribute no pre-cast time. Split copies do not multiply it.")
                .defineEnum("mode", ChantMode.MAXIMUM);
        builder.pop();
        builder.push("cooldowns");
        COOLDOWNS_ENABLED = builder.comment("Native Iron cooldowns for player casts containing Iron & Nouveau glyphs. Enabled by default.",
                "Shared with native Iron spellbooks and across Ars recipes; uses Iron cooldown reduction, events and synchronization.",
                "Check before chanting and again on release. Successful release starts each distinct spell's cooldown once.",
                "Existing projectiles, split branches and continuous effects keep running and paying mana normally.",
                "Includes active caster tools, bows/crossbows and Enchanter's Sword magic; automatic turrets and passive equipment are unchanged.",
                "Creative follows Iron's creative cooldown setting. False preserves previous bridge behavior.")
                .define("enabled", true);
        builder.pop();
        builder.push("movement");
        MOVEMENT_ENABLED = builder.comment("Use native Iron player movement restrictions during bridge chanting, player-anchored breaths and telekinesis.",
                "Restricts the actual breathing player, not a remote caster. Mob- and block-position breaths restrict nobody.",
                "Reuses Iron input scaling and CASTING_MOVESPEED. Non-player entities receive no additional restriction.",
                "Multiple bridge casts never multiply this penalty. False disables only bridge restrictions, not native Iron casting.")
                .define("enabled", true);
        builder.pop();
        builder.push("mana");
        USE_IRON_MANA = builder.comment("Pay player Iron glyph trigger costs from Iron's native mana pool. Enabled by default.",
                "False restores the Ars caster resource route. Does not change ordinary Ars glyph costs or Hex media costs.",
                "Split affordability and ongoing effects use the same selected pool; creative remains free.",
                "Non-player and fake-player casters retain their previous resource route.")
                .define("use_iron_mana", true);
        builder.pop();
        SPEC = builder.build();
    }
    private SpellLevelConfig() {}
    private static <T> T read(ModConfigSpec.ConfigValue<T> value) {
        return SPEC.isLoaded() ? value.get() : value.getDefault();
    }
    public static boolean requiresCrafting() { return read(LEARNING_MODE) == LearningMode.SCROLL_CRAFTING; }
    public static boolean chantingEnabled() { return read(CHANTING_ENABLED); }
    public static ChantMode chantMode() { return read(CHANTING_MODE); }
    public static boolean cooldownsEnabled() { return read(COOLDOWNS_ENABLED); }
    public static boolean movementEnabled() { return read(MOVEMENT_ENABLED); }
    public static boolean useIronMana() { return read(USE_IRON_MANA); }
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
                    levels.put(dev.ironsnouveau.platform.Locations.id(entry.substring(0, separator).trim()),
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
