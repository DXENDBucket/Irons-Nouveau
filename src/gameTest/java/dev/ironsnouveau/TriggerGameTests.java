package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentSplit;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.glyph.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.entity.spells.HealingAoe;
import io.redspace.ironsspellbooks.entity.mobs.SummonedVex;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(IronsNouveau.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TriggerGameTests {
    private static NativeFormAugment form(String id) { return IronsNouveau.forms().stream().filter(f -> f.spellId().getPath().equals(id)).findFirst().orElseThrow(); }
    private static BridgeGlyph effect(String id) { return IronsNouveau.glyphs().stream().filter(f -> f.definition().spellId().getPath().equals(id)).findFirst().orElseThrow(); }
    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        var cow = h.spawn(EntityType.COW, x, 2, z); cow.setNoAi(true); cow.setNoGravity(true); cow.setYRot(-90); return cow;
    }
    private static SpellResolver resolver(GameTestHelper h, LivingEntity caster, Spell spell, AtomicInteger balance) {
        var source = new LivingCaster(caster) {
            @Override public boolean enoughMana(int cost) { return balance.get() >= cost; }
            @Override public void expendMana(int cost) { balance.addAndGet(-cost); }
        };
        return new SpellResolver(new SpellContext(h.getLevel(), spell, caster, source, ItemStack.EMPTY));
    }
    private static void checkHits(HealingAoe field) {
        try {
            var method = io.redspace.ironsspellbooks.entity.spells.AoeEntity.class.getDeclaredMethod("checkHits");
            method.setAccessible(true); method.invoke(field);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    @GameTest(template = "empty", batch = "trigger_budget")
    public static void fiveShotsBecomeNativeThreeShotFan(GameTestHelper h) {
        var caster = cow(h, 1, 1); var balance = new AtomicInteger(100000);
        var parts = new ArrayList<AbstractSpellPart>(List.of(MethodProjectile.INSTANCE, form("icicle")));
        for (int i = 0; i < 4; i++) parts.add(AugmentSplit.INSTANCE);
        var spell = new Spell(parts); var resolver = resolver(h, caster, spell, balance);
        int nativeCost = SpellRegistry.getSpell(form("icicle").spellId()).getManaCost(1);
        balance.set(resolver.getExpendedCost() + 3 * nativeCost);
        h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "Cast accepted");
        var shots = h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, caster.getBoundingBox().inflate(12), p -> p.getOwner() == caster);
        h.assertTrue(shots.size() == 3, "Only three affordable projectiles spawn: " + shots.size());
        var angles = shots.stream().map(p -> Math.toDegrees(Math.atan2(p.getDeltaMovement().z, p.getDeltaMovement().x))).sorted().toList();
        h.assertTrue(Math.abs(angles.get(0) + 10) < 3 && Math.abs(angles.get(1)) < 3 && Math.abs(angles.get(2) - 10) < 3,
                "Ars generates its original three-shot fan: " + angles);
        h.assertTrue(balance.get() == 0, "Carrier cost and exactly three native costs; no overdraft: " + balance);
        for (var shot : shots) ((NativeCastCarrier)shot).ironsNouveau$session().finish(CastSession.EndReason.COMPLETED);
        h.succeed();
    }
    @GameTest(template = "empty", batch = "trigger_area", timeoutTicks = 30)
    public static void areaStopsAndResumesWithMana(GameTestHelper h) {
        var caster = cow(h, 3, 3); caster.setHealth(1);
        var balance = new AtomicInteger(100000); var glyph = effect("healing_circle");
        var resolver = resolver(h, caster, new Spell(MethodProjectile.INSTANCE, glyph), balance);
        int cost = SpellRegistry.getSpell(glyph.definition().spellId()).getManaCost(3);
        balance.set(cost * 2);
        glyph.onResolve(new EntityHitResult(caster), h.getLevel(), caster, new SpellStats.Builder().setAugments(java.util.Collections.nCopies(2, com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify.INSTANCE)).build(), resolver.spellContext, resolver);
        var field = h.getLevel().getEntitiesOfClass(HealingAoe.class, caster.getBoundingBox().inflate(5)).getFirst();
        h.assertTrue(field.getPersistentData().getCompound("irons_nouveau_lease").getInt("level") == 3, "Field persists its final native level");
        h.assertTrue(balance.get() == cost, "Creation pays once");
        checkHits(field); float after = caster.getHealth();
        h.assertTrue(after > 1 && balance.get() == 0, "First native pulse heals and pays");
        checkHits(field);
        h.assertTrue(balance.get() == 0, "Same native round does not pay per target");
        h.runAtTickTime(2, () -> {
            caster.setHealth(1); checkHits(field);
            h.assertTrue(caster.getHealth() == 1 && !field.isRemoved(), "No mana skips pulse without removing field");
            balance.set(cost); checkHits(field);
            h.assertTrue(caster.getHealth() > 1 && balance.get() == 0, "Replenishment allows next trigger");
            caster.discard();
        });
        h.runAtTickTime(4, () -> { h.assertTrue(field.isRemoved(), "Owner loss removes field"); h.succeed(); });
    }
    @GameTest(template = "empty", batch = "trigger_summon")
    public static void summonsUseHitPositionAndSinglePayment(GameTestHelper h) {
        var caster = cow(h, 1, 1); var target = cow(h, 5, 5);
        var glyph = effect("summon_vex"); var balance = new AtomicInteger(100000);
        var resolver = resolver(h, caster, new Spell(MethodProjectile.INSTANCE, glyph), balance);
        int cost = SpellRegistry.getSpell(glyph.definition().spellId()).getManaCost(3); balance.set(cost);
        glyph.onResolve(new EntityHitResult(target), h.getLevel(), caster, new SpellStats.Builder().setAugments(java.util.Collections.nCopies(2, com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify.INSTANCE)).build(), resolver.spellContext, resolver);
        var summons = h.getLevel().getEntitiesOfClass(SummonedVex.class, target.getBoundingBox().inflate(5));
        h.assertTrue(summons.size() == 5 && balance.get() == 0, "Level three summons five native Vex with one payment: " + summons.size());
        for (var mob : summons) {
            h.assertTrue(mob.distanceTo(target) < 4 && mob.getTarget() == target, "Summon uses Ars hit and enemy");
            h.assertTrue(mob.getPersistentData().getCompound("irons_nouveau_lease").getUUID("owner").equals(caster.getUUID()), "Owner remains caster");
            mob.discard();
        }
        h.succeed();
    }
    @GameTest(template = "empty", batch = "trigger_complex")
    public static void thunderstormRetainsLevelForUpkeepAndPermission(GameTestHelper h) {
        var caster = cow(h, 1, 1); var target = cow(h, 3, 1);
        var glyph = effect("thunderstorm");
        var spell = (io.redspace.ironsspellbooks.spells.lightning.ThunderstormSpell)SpellRegistry.getSpell(glyph.definition().spellId());
        int cost = spell.getManaCost(3); var balance = new AtomicInteger(cost * 2);
        var resolver = resolver(h, caster, new Spell(MethodProjectile.INSTANCE, glyph), balance);
        glyph.onResolve(new EntityHitResult(target), h.getLevel(), caster,
                new SpellStats.Builder().setAugments(Collections.nCopies(2, com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify.INSTANCE)).build(), resolver.spellContext, resolver);
        var status = target.getEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.THUNDERSTORM);
        h.assertTrue(status != null && status.getDuration() == spell.getDurationTicks(3, caster)
                && status.getAmplifier() == 10 && balance.get() == cost, "Native level three duration, strength and initial fee");
        var deniedLevel = new AtomicInteger();
        java.util.function.Consumer<dev.ironsnouveau.api.GlyphAccessEvent> deny = event -> {
            if (event.actor() == caster) { deniedLevel.set(event.spellLevel()); event.setCanceled(true); }
        };
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(deny);
        try {
            StatusBilling.pulse(target, () -> { throw new AssertionError("Denied upkeep ran"); });
            h.assertTrue(deniedLevel.get() == 3 && balance.get() == cost, "Upkeep permission uses stored level and denial costs nothing");
        } finally { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(deny); }
        var pulses = new AtomicInteger();
        StatusBilling.pulse(target, () -> { pulses.incrementAndGet(); return true; });
        StatusBilling.pulse(target, () -> { pulses.incrementAndGet(); return true; });
        h.assertTrue(pulses.get() == 1 && balance.get() == 0, "Exactly one affordable upkeep at level three");
        target.discard(); caster.discard(); h.succeed();
    }
    @GameTest(template = "empty", batch = "trigger_complex")
    public static void complexFormsCreateNativePayloads(GameTestHelper h) {
        var caster = cow(h, 1, 1); var balance = new AtomicInteger(100000);
        for (String id : List.of("fireball", "fire_arrow", "poison_arrow", "magma_bomb", "snowball", "acid_orb")) {
            var resolver = resolver(h, caster, new Spell(MethodProjectile.INSTANCE, form(id)), balance);
            h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "Complex cast accepted");
            var shot = h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, caster.getBoundingBox().inflate(12), p -> p.getOwner() == caster).getFirst();
            var session = ((NativeCastCarrier)shot).ironsNouveau$session();
            var hit = new BlockHitResult(caster.position().add(3, 0, 0), net.minecraft.core.Direction.UP, caster.blockPosition().east(3).below(), false);
            ((dev.ironsnouveau.mixin.NativeProjectileHitAccess)shot).ironsNouveau$hit(hit);
            session.finish(CastSession.EndReason.COMPLETED);
        }
        h.succeed();
    }
    @GameTest(template = "empty", batch = "trigger_shared_pool")
    public static void playerUsesArsSpellsSharedPool(GameTestHelper h) throws Exception {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "NouveauManaTest"));
        player.setPos(h.absoluteVec(new Vec3(1, 2, 1))); player.setYRot(-90);
        player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA).setBaseValue(10000);
        var mode = com.otectus.arsnspells.bridge.BridgeManager.getCurrentMode();
        var setter = com.otectus.arsnspells.bridge.BridgeManager.class.getDeclaredMethod("testSetMode", com.otectus.arsnspells.config.ManaUnificationMode.class);
        setter.setAccessible(true);
        try {
            setter.invoke(null, com.otectus.arsnspells.config.ManaUnificationMode.ISS_PRIMARY);
            var iron = io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player);
            var ars = com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry.getMana(player);
            iron.setMana(1000);
            var account = TriggerMana.of(null, player); int cost = SpellRegistry.getSpell(form("icicle").spellId()).getManaCost(1);
            h.assertTrue(account.trigger(form("icicle").spellId(), 1, () -> true), "Shared-pool activation succeeds");
            h.assertTrue(Math.abs(iron.getMana() - (1000 - cost)) < .01 && Math.abs(ars.getCurrentMana() - iron.getMana()) < .01,
                    "Iron and Ars show the same remaining pool");
            iron.setMana(cost - 1);
            h.assertTrue(!account.trigger(form("icicle").spellId(), 1, () -> { throw new AssertionError("Unaffordable action executed"); }), "Shared-pool shortage blocks effect");
            h.assertTrue(iron.getMana() == cost - 1, "Failed trigger leaves shared mana unchanged");
            player.setGameMode(net.minecraft.world.level.GameType.CREATIVE); iron.setMana(0);
            var parts = new ArrayList<AbstractSpellPart>(List.of(MethodProjectile.INSTANCE, form("icicle")));
            for (int i = 0; i < 4; i++) parts.add(AugmentSplit.INSTANCE);
            var creativeSpell = new Spell(parts);
            var creativeResolver = new SpellResolver(new SpellContext(h.getLevel(), creativeSpell, player, LivingCaster.from(player), ItemStack.EMPTY));
            h.assertTrue(creativeResolver.onCast(ItemStack.EMPTY, h.getLevel()), "Zero-mana creative cast succeeds");
            var shots = h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, player.getBoundingBox().inflate(12), p -> p.getOwner() == player);
            h.assertTrue(shots.size() == 5 && iron.getMana() == 0, "Creative retains all five shots and does not pay");
            for (var shot : shots) ((NativeCastCarrier)shot).ironsNouveau$session().finish(CastSession.EndReason.COMPLETED);
            h.assertTrue(account.trigger(form("icicle").spellId(), 1, () -> true), "Zero-mana creative repeated activation succeeds");
        } finally { setter.invoke(null, mode); player.discard(); }
        h.succeed();
    }
}
