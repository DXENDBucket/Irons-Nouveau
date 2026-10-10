package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellSchools;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import dev.ironsnouveau.api.GlyphDefinition;
import dev.ironsnouveau.api.SpellAdapter;
import dev.ironsnouveau.bridge.NativeAdapters;
import dev.ironsnouveau.bridge.TargetedSpellAdapters;
import dev.ironsnouveau.bridge.WorldSpellAdapters;
import dev.ironsnouveau.bridge.SummoningAdapters;
import dev.ironsnouveau.casting.ComplexProjectilePayload;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.glyph.NativeFormAugment;
import dev.ironsnouveau.casting.ProjectileCastAdapter;
import dev.ironsnouveau.casting.ProjectilePayload;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentDampen;
import io.redspace.ironsspellbooks.entity.spells.firebolt.FireboltProjectile;
import io.redspace.ironsspellbooks.entity.spells.icicle.IcicleProjectile;
import io.redspace.ironsspellbooks.entity.spells.magic_missile.MagicMissileProjectile;
import io.redspace.ironsspellbooks.entity.spells.guiding_bolt.GuidingBoltProjectile;
import io.redspace.ironsspellbooks.entity.spells.lightning_lance.LightningLanceProjectile;
import io.redspace.ironsspellbooks.entity.spells.magic_arrow.MagicArrowProjectile;
import io.redspace.ironsspellbooks.entity.spells.blood_needle.BloodNeedle;
import java.util.ArrayList;
import java.util.List;

/** One glyph catalogue for both Minecraft targets. Loader initialization stays outside this class. */
public final class GlyphCatalog {
    public static final String MOD_ID = "irons_nouveau";
    private static final List<BridgeGlyph> GLYPHS = new ArrayList<>();
    private static final List<NativeFormAugment> FORMS = new ArrayList<>();
    private GlyphCatalog() {}
    public static void register() {
        AdditionalGlyphs.register();
        ExpansionGlyphs.register();
        addForm(new NativeFormAugment("ball_lightning", "Ball Lightning", SpellSchools.ELEMENTAL_AIR,
                new ProjectileCastAdapter(io.redspace.ironsspellbooks.entity.spells.ball_lightning.BallLightning::new, ProjectilePayload.HALF_POWER), 0, SpellTier.ONE));
        addForm(new NativeFormAugment("flaming_barrage", "Flaming Barrage", SpellSchools.ELEMENTAL_FIRE,
                new ProjectileCastAdapter(io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball::new, ProjectilePayload.FLAMING_BARRAGE), 0, SpellTier.ONE));
        add("fire_breath", "Fire Breath", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_FIRE, true, true, dev.ironsnouveau.bridge.BreathAdapters.of("fire_breath"));
        add("poison_breath", "Poison Breath", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_EARTH, true, true, dev.ironsnouveau.bridge.BreathAdapters.of("poison_breath"));
        add("ray_of_siphoning", "Ray of Siphoning", 0, SpellTier.ONE, dev.ironsnouveau.platform.GlyphSchools.NECROMANCY, true, true, dev.ironsnouveau.bridge.RayAdapters.SIPHON);
        add("ray_of_frost", "Ray of Frost", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_WATER, true, false, dev.ironsnouveau.bridge.RayAdapters.FROST);
        add("electrocute", "Electrocute", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_AIR, true, true, dev.ironsnouveau.bridge.BreathAdapters.of("electrocute"));
        add("sunbeam", "Sunbeam", 0, SpellTier.ONE, SpellSchools.ABJURATION, true, false, dev.ironsnouveau.bridge.PointEntityAdapters.SUNBEAM);
        add("dragon_breath", "Dragon Breath", 0, SpellTier.ONE, SpellSchools.MANIPULATION, true, true, dev.ironsnouveau.bridge.BreathAdapters.of("dragon_breath"));
        add("cone_of_cold", "Cone of Cold", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_WATER, true, true, dev.ironsnouveau.bridge.BreathAdapters.of("cone_of_cold"));
        add("heal", "Heal", 30, SpellTier.ONE, SpellSchools.ABJURATION, false, false, NativeAdapters.HEAL);
        add("fortify", "Fortify", 80, SpellTier.ONE, SpellSchools.ABJURATION, false, true, NativeAdapters.FORTIFY);
        add("oakskin", "Oakskin", 25, SpellTier.ONE, SpellSchools.ELEMENTAL_EARTH, false, true, NativeAdapters.OAKSKIN);
        add("haste", "Haste", 50, SpellTier.ONE, SpellSchools.ABJURATION, false, true, TargetedSpellAdapters.HASTE);
        add("slow", "Slow", 50, SpellTier.ONE, SpellSchools.CONJURATION, true, true, TargetedSpellAdapters.SLOW);
        add("blight", "Blight", 60, SpellTier.ONE, SpellSchools.ELEMENTAL_EARTH, true, true, TargetedSpellAdapters.BLIGHT);
        add("greater_heal", "Greater Heal", 0, SpellTier.ONE, SpellSchools.ABJURATION, false, false, WorldSpellAdapters.GREATER_HEAL);
        add("cleanse", "Cleanse", 0, SpellTier.ONE, SpellSchools.ABJURATION, false, false, WorldSpellAdapters.CLEANSE);
        add("lightning_bolt", "Lightning Bolt", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_AIR, true, false, WorldSpellAdapters.LIGHTNING);
        add("eldritch_blast", "Eldritch Blast", 0, SpellTier.ONE, dev.ironsnouveau.platform.GlyphSchools.NECROMANCY, true, false, WorldSpellAdapters.ELDRITCH_BLAST);
        add("chain_lightning", "Chain Lightning", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_AIR, true, false, WorldSpellAdapters.CHAIN);
        add("healing_circle", "Healing Circle", 0, SpellTier.ONE, SpellSchools.ABJURATION, false, true, WorldSpellAdapters.HEALING_CIRCLE);
        add("black_hole", "Black Hole", 0, SpellTier.ONE, SpellSchools.MANIPULATION, true, true, WorldSpellAdapters.BLACK_HOLE);
        add("sculk_tentacles", "Sculk Tentacles", 0, SpellTier.ONE, dev.ironsnouveau.platform.GlyphSchools.NECROMANCY, true, false, WorldSpellAdapters.TENTACLES);
        add("root", "Root", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_EARTH, true, true, WorldSpellAdapters.ROOT);
        add("wisp", "Wisp", 0, SpellTier.ONE, SpellSchools.ABJURATION, true, false, WorldSpellAdapters.WISP);
        add("firefly_swarm", "Firefly Swarm", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_EARTH, true, false, WorldSpellAdapters.FIREFLIES);
        add("thunderstorm", "Thunderstorm", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_AIR, false, true, WorldSpellAdapters.THUNDERSTORM);
        add("summon_vex", "Summon Vex", 0, SpellTier.ONE, SpellSchools.CONJURATION, false, true, SummoningAdapters.forSpell("summon_vex"));
        add("raise_dead", "Raise Dead", 0, SpellTier.ONE, dev.ironsnouveau.platform.GlyphSchools.NECROMANCY, false, true, SummoningAdapters.forSpell("raise_dead"));
        add("summon_horse", "Summon Horse", 0, SpellTier.ONE, SpellSchools.CONJURATION, false, true, SummoningAdapters.forSpell("summon_horse"));
        add("summon_swords", "Summon Swords", 0, SpellTier.ONE, SpellSchools.MANIPULATION, false, true, SummoningAdapters.forSpell("summon_swords"));
        add("summon_polar_bear", "Summon Polar Bear", 0, SpellTier.ONE, SpellSchools.ELEMENTAL_WATER, false, true, SummoningAdapters.forSpell("summon_polar_bear"));
        addForm(new NativeFormAugment("firebolt", "Firebolt", SpellSchools.ELEMENTAL_FIRE, new ProjectileCastAdapter(FireboltProjectile::new)));
        addForm(new NativeFormAugment("icicle", "Icicle", SpellSchools.ELEMENTAL_WATER, new ProjectileCastAdapter(IcicleProjectile::new)));
        addForm(new NativeFormAugment("magic_missile", "Magic Missile", SpellSchools.MANIPULATION, new ProjectileCastAdapter(MagicMissileProjectile::new)));
        addForm(new NativeFormAugment("guiding_bolt", "Guiding Bolt", SpellSchools.ABJURATION,
                new ProjectileCastAdapter(GuidingBoltProjectile::new, ProjectilePayload.GUIDING_BOLT), 20, SpellTier.ONE));
        addForm(new NativeFormAugment("lightning_lance", "Lightning Lance", SpellSchools.ELEMENTAL_AIR,
                new ProjectileCastAdapter(LightningLanceProjectile::new, ProjectilePayload.FULL_POWER), 50, SpellTier.ONE));
        addForm(new NativeFormAugment("magic_arrow", "Magic Arrow", SpellSchools.MANIPULATION,
                new ProjectileCastAdapter(MagicArrowProjectile::new, ProjectilePayload.FULL_POWER), 40, SpellTier.ONE));
        addForm(new NativeFormAugment("blood_needles", "Blood Needles", dev.ironsnouveau.platform.GlyphSchools.NECROMANCY,
                new ProjectileCastAdapter(BloodNeedle::new, ProjectilePayload.BLOOD_NEEDLE), 10, SpellTier.ONE));
        complex("fireball", "Fireball", SpellSchools.ELEMENTAL_FIRE, SpellTier.ONE);
        complex("fire_arrow", "Fire Arrow", SpellSchools.ELEMENTAL_FIRE, SpellTier.ONE);
        complex("poison_arrow", "Poison Arrow", SpellSchools.ELEMENTAL_EARTH, SpellTier.ONE);
        complex("magma_bomb", "Magma Bomb", SpellSchools.ELEMENTAL_FIRE, SpellTier.ONE);
        complex("snowball", "Snowball", SpellSchools.ELEMENTAL_WATER, SpellTier.ONE);
        complex("acid_orb", "Acid Orb", SpellSchools.ELEMENTAL_EARTH, SpellTier.ONE);
        complex("wither_skull", "Wither Skull", dev.ironsnouveau.platform.GlyphSchools.NECROMANCY, SpellTier.ONE);
        complex("lob_creeper", "Lob Creeper", SpellSchools.ELEMENTAL_AIR, SpellTier.ONE);
        addForm(new NativeFormAugment("blood_slash", "Blood Slash", dev.ironsnouveau.platform.GlyphSchools.NECROMANCY,
                new ProjectileCastAdapter(io.redspace.ironsspellbooks.entity.spells.blood_slash.BloodSlashProjectile::new, ProjectilePayload.FULL_POWER), 0, SpellTier.ONE));
    }
    public static void configureCarriers() {
            for (var profile : dev.arsconflux.api.projectile.CarrierRegistry.profiles()) {
                var glyph = profile.glyph();
                if (glyph == null) continue;
                profile.rememberOriginalAugments(glyph);
                glyph.compatibleAugments.addAll(FORMS);
                glyph.compatibleAugments.addAll(profile.augments());
                glyph.compatibleAugments.add(AugmentAmplify.INSTANCE);
                glyph.compatibleAugments.add(AugmentDampen.INSTANCE);
                dev.ironsnouveau.platform.GlyphHints.carrier(glyph, FORMS);
            }
    }
    public static List<BridgeGlyph> glyphs() { return List.copyOf(GLYPHS); }
    public static List<NativeFormAugment> forms() { return List.copyOf(FORMS); }
    private static void addForm(NativeFormAugment form) { GlyphRegistry.registerSpell(form); FORMS.add(form); }
    private static void complex(String id, String name, SpellSchool school, SpellTier tier) {
        var payload = new ComplexProjectilePayload(id);
        addForm(new NativeFormAugment(id, name, school, new ProjectileCastAdapter(payload::create, payload), 0, tier));
    }
    static void add(String spell, String name, int cost, SpellTier tier, SpellSchool school,
                            boolean harmful, boolean duration, SpellAdapter adapter) {
        var definition = new GlyphDefinition(dev.ironsnouveau.platform.Locations.id(MOD_ID, "glyph_" + spell),
                dev.ironsnouveau.platform.Locations.id("irons_spellbooks", spell), name, 1, cost, tier,
                school, harmful, duration, adapter);
        var glyph = new BridgeGlyph(definition);
        GlyphRegistry.registerSpell(glyph);
        GLYPHS.add(glyph);
    }
}
