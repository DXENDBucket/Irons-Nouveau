package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.progression.SpellProgress;
import io.redspace.ironsspellbooks.api.magic.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.events.SpellCooldownAddedEvent;
import io.redspace.ironsspellbooks.capabilities.magic.MagicManager;
import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public final class RecastCooldownCases {
    private static AbstractSpellPart part(String id) {
        return java.util.stream.Stream.concat(IronsNouveau.forms().stream(), IronsNouveau.glyphs().stream())
                .filter(p -> BoundSpellSupport.ironSpell(p).getPath().equals(id)).findFirst().orElseThrow();
    }
    private static AbstractSpell nativeSpell(String id) { return SpellRegistry.getSpell(BoundSpellSupport.ironSpell(part(id))); }
    private static void learn(ServerPlayer p, AbstractSpell spell) {
        var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(spell, 1, scroll); SpellProgress.crafted(p, scroll);
    }
    public static void actualSingleShotAndNativeEvents(GameTestHelper h, ServerPlayer p,
            Consumer<Consumer<SpellCooldownAddedEvent.Pre>> register,
            Consumer<Consumer<SpellCooldownAddedEvent.Pre>> unregister) {
        boolean enabled = SpellLevelConfig.COOLDOWNS_ENABLED.get(), chanting = SpellLevelConfig.CHANTING_ENABLED.get();
        SpellLevelConfig.COOLDOWNS_ENABLED.set(true); SpellLevelConfig.CHANTING_ENABLED.set(false);
        var barrage = nativeSpell("flaming_barrage"); learn(p, barrage);
        var recipe = new Spell(MethodProjectile.INSTANCE, part("flaming_barrage"));
        var cds = MagicData.getPlayerMagicData(p).getPlayerCooldowns();
        var seen = new AtomicInteger(-1);
        Consumer<SpellCooldownAddedEvent.Pre> listener = e -> { if (e.getEntity() == p) seen.set(e.getEffectiveCooldown()); };
        register.accept(listener);
        try {
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK)); p.setXRot(-90);
            int nativeTicks = MagicManager.getEffectiveSpellCooldown(barrage, p, CastSource.SPELLBOOK);
            int expected = RecastCooldowns.perShot(nativeTicks, 5);
            int before = h.getLevel().getEntitiesOfClass(SmallMagicFireball.class, p.getBoundingBox().inflate(20), b -> b.getOwner() == p).size();
            var resolver = new SpellResolver(SpellContext.fromEntity(recipe, p, p.getMainHandItem()));
            boolean result = ActiveCooldowns.execute(recipe, p, CastSource.SPELLBOOK,
                    () -> resolver.onCast(p.getMainHandItem(), h.getLevel()), false);
            h.assertTrue(result && h.getLevel().getEntitiesOfClass(SmallMagicFireball.class, p.getBoundingBox().inflate(20), b -> b.getOwner() == p).size() == before + 1,
                    "Actual release emits exactly one native barrage fireball");
            h.assertTrue(cds.getSpellCooldowns().get(barrage.getSpellId()).getCooldownRemaining() == expected && seen.get() == expected,
                    "Native events/storage receive one fifth of the effective cooldown");
            h.assertTrue(!ActiveCooldowns.allowed(recipe, p), "Shared native cooldown still blocks a new cast");
            cds.tick(expected); h.assertTrue(ActiveCooldowns.allowed(recipe, p), "Shortened native cooldown expires normally");
            int swordTicks = MagicManager.getEffectiveSpellCooldown(barrage, p, CastSource.SWORD);
            RecastCooldowns.add(p, barrage, CastSource.SWORD, 5);
            h.assertTrue(seen.get() == RecastCooldowns.perShot(swordTicks, 5), "Native weapon multiplier is included before division");
            cds.clearCooldowns(); MagicHelper.MAGIC_MANAGER.addCooldown(p, barrage, CastSource.SPELLBOOK);
            h.assertTrue(seen.get() == nativeTicks && cds.getSpellCooldowns().get(barrage.getSpellId()).getCooldownRemaining() == nativeTicks,
                    "Unscoped original Iron casting keeps its full native cooldown");
            h.succeed();
        } finally { unregister.accept(listener); p.discard(); SpellLevelConfig.COOLDOWNS_ENABLED.set(enabled); SpellLevelConfig.CHANTING_ENABLED.set(chanting); }
    }
    public static void resolvedLevelsAndEventOverrides(GameTestHelper h, ServerPlayer p,
            Consumer<Consumer<SpellCooldownAddedEvent.Pre>> register,
            Consumer<Consumer<SpellCooldownAddedEvent.Pre>> unregister) {
        var blast = nativeSpell("eldritch_blast"); var barrage = nativeSpell("flaming_barrage");
        var blastPart = part("eldritch_blast");
        var low = new Spell(MethodSelf.INSTANCE, blastPart);
        var high = new Spell(MethodSelf.INSTANCE, blastPart, AugmentAmplify.INSTANCE, AugmentAmplify.INSTANCE);
        var stack = new ItemStack(Items.STICK); BoundSpellSupport.bind(stack, high, 4);
        try {
            learn(p, blast);
            h.assertTrue(RecastCooldowns.occurrences(List.of(low), p).get(blast) == 3, "Level one native blast allows three shots");
            h.assertTrue(RecastCooldowns.occurrences(List.of(high), p).get(blast) == 5, "Amplifies increase resolved blast level and shot count");
            h.assertTrue(RecastCooldowns.occurrences(List.of(low, high), p).get(blast) == 3,
                    "Duplicate levels use one shared longest cooldown, independent of recipe order");
            int bound = PresetTools.scoped(new PresetTools.Token(p, stack), () -> RecastCooldowns.occurrences(List.of(high), p).get(blast));
            h.assertTrue(bound == 8, "Fixed bound level and augments determine the actual native shot count");
            for (String id : List.of("summon_vex", "fire_breath", "ray_of_siphoning", "firebolt")) {
                var spell = nativeSpell(id);
                h.assertTrue(RecastCooldowns.shots(spell, 5, p) == 1, "Recall/channel/ordinary attacks keep full cooldown: " + id);
            }
            h.assertTrue(RecastCooldowns.perShot(1, 5) == 1 && RecastCooldowns.perShot(0, 5) == 0,
                    "Rounding cannot create a zero cooldown from a positive one");
            Consumer<SpellCooldownAddedEvent.Pre> override = e -> { if (e.getEntity() == p) e.setEffectiveCooldown(7); };
            register.accept(override);
            try { RecastCooldowns.add(p, barrage, CastSource.SPELLBOOK, 5);
                h.assertTrue(MagicData.getPlayerMagicData(p).getPlayerCooldowns().getSpellCooldowns().get(barrage.getSpellId()).getCooldownRemaining() == 7,
                        "Native event override is final and is not divided a second time");
            } finally { unregister.accept(override); }
            MagicData.getPlayerMagicData(p).getPlayerCooldowns().clearCooldowns();
            Consumer<SpellCooldownAddedEvent.Pre> cancel = e -> { if (e.getEntity() == p) e.setCanceled(true); };
            register.accept(cancel);
            try { RecastCooldowns.add(p, barrage, CastSource.SPELLBOOK, 5);
                h.assertTrue(!MagicData.getPlayerMagicData(p).getPlayerCooldowns().isOnCooldown(barrage), "Native event cancellation is respected");
            } finally { unregister.accept(cancel); }
            h.assertTrue(MagicManager.getEffectiveSpellCooldown(barrage, p, CastSource.SPELLBOOK) == barrage.getSpellCooldown(),
                    "Release scope is cleaned after callbacks");
            h.succeed();
        } finally { p.discard(); }
    }
}
