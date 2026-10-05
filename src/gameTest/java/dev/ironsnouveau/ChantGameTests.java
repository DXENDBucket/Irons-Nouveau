package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHeal;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import dev.ironsnouveau.casting.ActiveChanting;
import dev.ironsnouveau.casting.ChantTiming;
import dev.ironsnouveau.casting.SpellLevels;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.glyph.BridgeGlyph;
import io.redspace.ironsspellbooks.api.registry.AttributeRegistry;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("irons_nouveau_chant")
@PrefixGameTestTemplate(false)
public final class ChantGameTests {
    private static BridgeGlyph glyph(String id) {
        return IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow();
    }
    @GameTest(template = "empty", batch = "chant_timing")
    public static void nativeTimingIncludesLevelsAndReduction(GameTestHelper h) {
        var player = ScrollProgressGameTests.player(h);
        var heal = glyph("summon_vex");
        var form = IronsNouveau.forms().stream().filter(g -> g.spellId().getPath().equals("fireball")).findFirst().orElseThrow();
        var recipe = new Spell(MethodProjectile.INSTANCE, form, AugmentAmplify.INSTANCE, heal, heal);
        var nativeHeal = SpellRegistry.getSpell(heal.definition().spellId());
        var nativeBolt = SpellRegistry.getSpell(form.spellId());
        int healLevel = SpellLevels.resolve(player, heal.definition().spellId(), List.of());
        int boltLevel = SpellLevels.resolve(player, form.spellId(), List.of(AugmentAmplify.INSTANCE));
        for (double reduction : new double[]{1, 1.5}) {
            player.getAttribute(AttributeRegistry.CAST_TIME_REDUCTION).setBaseValue(reduction);
            int a = nativeHeal.getEffectiveCastTime(healLevel, player);
            int b = nativeBolt.getEffectiveCastTime(boltLevel, player);
            h.assertTrue(a > 0 && b > 0, "Fixtures must have native long casts");
            h.assertTrue(ChantTiming.ticks(recipe, player, SpellLevelConfig.ChantMode.MAXIMUM) == Math.max(a, b), "MAX uses native effective duration");
            h.assertTrue(ChantTiming.ticks(recipe, player, SpellLevelConfig.ChantMode.SUM) == a * 2 + b, "SUM counts occurrences, using each phrase's level");
        }
        h.assertTrue(ChantTiming.ticks(new Spell(MethodSelf.INSTANCE, glyph("fire_breath")), player, SpellLevelConfig.ChantMode.SUM) == 0, "Channel duration is not pre-cast delay");
        h.assertTrue(ChantTiming.ticks(new Spell(MethodSelf.INSTANCE, glyph("heal")), player, SpellLevelConfig.ChantMode.SUM) == 0, "Native instant spells stay instant");
        player.discard(); h.succeed();
    }
    @GameTest(template = "empty", batch = "chant_queue", timeoutTicks = 240)
    public static void activeBoundaryAndCancellation(GameTestHelper h) {
        var player = ScrollProgressGameTests.player(h);
        var stack = new ItemStack(Items.STICK);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var iron = new Spell(MethodSelf.INSTANCE, glyph("summon_vex"));
        var pure = new Spell(MethodSelf.INSTANCE, EffectHeal.INSTANCE);
        var count = new AtomicInteger();
        // Exercise the real woven AbstractCaster entry, stopping at the dispatch boundary.
        // The recipe's native damage/AI behaviour is outside the scope of a pre-cast test.
        var tool = new SpellCaster() {
            @Override public SpellResolver getSpellResolver(SpellContext context, net.minecraft.world.level.Level world,
                    net.minecraft.world.entity.LivingEntity entity, InteractionHand hand) {
                return new SpellResolver(context) {
                    @Override public boolean onCast(ItemStack item, net.minecraft.world.level.Level level) { count.incrementAndGet(); return true; }
                    @Override public boolean onCastOnBlock(net.minecraft.world.phys.BlockHitResult hit) { count.incrementAndGet(); return true; }
                    @Override public boolean onCastOnBlock(net.minecraft.world.item.context.UseOnContext hit) { count.incrementAndGet(); return true; }
                    @Override public boolean onCastOnEntity(ItemStack item, net.minecraft.world.entity.Entity target, InteractionHand hand) { count.incrementAndGet(); return true; }
                };
            }
        };
        player.setXRot(-90);
        SpellLevelConfig.CHANTING_ENABLED.set(true);
        SpellLevelConfig.CHANTING_MODE.set(SpellLevelConfig.ChantMode.MAXIMUM);
        h.assertTrue(!ActiveChanting.defer(pure, player, InteractionHand.MAIN_HAND, count::incrementAndGet), "Pure Ars bypasses chanting");
        tool.castSpell(h.getLevel(), player, InteractionHand.MAIN_HAND, null, pure);
        h.assertTrue(count.get() == 1 && !ActiveChanting.isChanting(player), "Pure Ars active dispatch remains immediate");
        SpellLevelConfig.CHANTING_ENABLED.set(false);
        h.assertTrue(!ActiveChanting.defer(iron, player, InteractionHand.MAIN_HAND, count::incrementAndGet), "Disabled mode bypasses chanting");
        tool.castSpell(h.getLevel(), player, InteractionHand.MAIN_HAND, null, iron);
        h.assertTrue(count.get() == 2 && !ActiveChanting.isChanting(player), "Disabled active dispatch remains immediate");
        count.set(0);
        SpellLevelConfig.CHANTING_ENABLED.set(true);
        int ticks = ChantTiming.ticks(iron, player, SpellLevelConfig.ChantMode.MAXIMUM);
        tool.castSpell(h.getLevel(), player, InteractionHand.MAIN_HAND, null, iron);
        h.assertTrue(ActiveChanting.isChanting(player) && count.get() == 0, "Real active entry delays the resolver");
        h.assertTrue(ActiveChanting.defer(iron, player, InteractionHand.MAIN_HAND, () -> count.addAndGet(100)), "Repeated click does not enqueue another cast");
        h.assertTrue(!ActiveChanting.defer(pure, player, InteractionHand.MAIN_HAND, count::incrementAndGet), "Even an existing chant cannot intercept pure Ars");
        h.runAfterDelay(ticks - 1, () -> h.assertTrue(count.get() == 0, "No early execution"));
        h.runAfterDelay(ticks + 2, () -> {
            h.assertTrue(count.get() == 1 && !ActiveChanting.isChanting(player), "Release exactly once");
            ActiveChanting.defer(iron, player, InteractionHand.MAIN_HAND, count::incrementAndGet);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
        });
        h.runAfterDelay(ticks + 4, () -> {
            h.assertTrue(!ActiveChanting.isChanting(player) && count.get() == 1, "Switching the casting item cancels without releasing");
            player.discard(); h.succeed();
        });
    }
}
