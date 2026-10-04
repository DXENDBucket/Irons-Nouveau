package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectLight;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.glyph.BridgeGlyph;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.*;
import io.redspace.ironsspellbooks.entity.spells.ice_tomb.IceTombEntity;
import io.redspace.ironsspellbooks.entity.spells.poison_cloud.PoisonCloud;
import io.redspace.ironsspellbooks.entity.spells.poison_cloud.PoisonSplash;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(IronsNouveau.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AdditionalSpellsGameTests {
    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        var cow = h.spawn(EntityType.COW, x, 2, z); cow.setNoAi(true); cow.setNoGravity(true); cow.setYRot(-90);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); cow.setHealth(1000);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); return cow;
    }
    private static BridgeGlyph glyph(String id) { return IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow(); }
    private static SpellResolver resolver(GameTestHelper h, LivingEntity caster, Spell spell, AtomicInteger mana) {
        return new SpellResolver(new SpellContext(h.getLevel(), spell, caster, new LivingCaster(caster) {
            @Override public boolean enoughMana(int amount) { return mana.get() >= amount; }
            @Override public void expendMana(int amount) { mana.addAndGet(-amount); }
        }, ItemStack.EMPTY));
    }
    private static void cast(GameTestHelper h, LivingEntity caster, String id, HitResult hit, AtomicInteger mana, int level) {
        var glyph = glyph(id); var resolver = resolver(h, caster, new Spell(MethodProjectile.INSTANCE, glyph), mana);
        var stats = new SpellStats.Builder().setAugments(Collections.nCopies(level - 1, AugmentAmplify.INSTANCE)).build();
        glyph.onResolve(hit, h.getLevel(), caster, stats, resolver.spellContext, resolver);
    }
    private static BlockHitResult at(GameTestHelper h, int x, int y, int z) {
        var pos = h.absolutePos(new BlockPos(x, y, z)); return new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
    }
    @GameTest(template = "empty", batch = "additional_status")
    public static void nativeStatusesUseTargetAndResolvedLevel(GameTestHelper h) {
        var caster = cow(h, 1, 1); var target = cow(h, 4, 1); var mana = new AtomicInteger(100000);
        String[] ids = {"heartstop", "abyssal_shroud", "planar_sight", "invisibility", "angel_wing", "frostbite", "charge", "gluttony", "spider_aspect", "echoing_strikes"};
        for (var id : ids) {
            int before = mana.get(); cast(h, caster, id, new EntityHitResult(target), mana, 3);
            h.assertTrue(before - mana.get() == SpellRegistry.getSpell(ResourceLocation.parse("irons_spellbooks:" + id)).getManaCost(3), id + " pays the level-three native fee");
        }
        h.assertTrue(target.hasEffect(MobEffectRegistry.ANGEL_WINGS) && !caster.hasEffect(MobEffectRegistry.ANGEL_WINGS), "Recipient is the Ars target");
        h.assertTrue(target.getEffect(MobEffectRegistry.CHARGED).getAmplifier() == 2 && target.getEffect(MobEffectRegistry.FROSTBITTEN_STRIKES).getAmplifier() == 7, "Native level-specific amplifiers");
        h.assertTrue(io.redspace.ironsspellbooks.effect.EchoingStrikesData.get(target).getHitCount() == 5, "Echo hit budget initialized on target");
        caster.discard(); target.discard(); h.succeed();
    }
    @GameTest(template = "empty", batch = "additional_points")
    public static void pointSpellsProduceManagedNativeEntities(GameTestHelper h) {
        var caster = cow(h, 1, 1); var mana = new AtomicInteger(100000);
        for (var pos : BlockPos.betweenClosed(2, 0, 2, 14, 0, 14)) h.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE);
        String[] ids = {"ice_block", "shield", "scapegoat", "stomp", "fang_strike", "fang_ward", "earthquake", "blizzard", "gravity_fissure", "scorch"};
        for (var id : ids) {
            var hit = at(h, 7, 1, 7); int before = mana.get(); cast(h, caster, id, hit, mana, 3);
            h.assertTrue(mana.get() < before, id + " creates a native entity");
            var entities = h.getLevel().getEntities(caster, new AABB(hit.getLocation(), hit.getLocation()).inflate(15), e ->
                    e.getPersistentData().getCompound("irons_nouveau_lease").getString("spell").equals("irons_spellbooks:" + id));
            h.assertTrue(!entities.isEmpty(), id + " carries a lease, including vanilla-typed fangs");
            for (var entity : entities) {
                var lease = entity.getPersistentData().getCompound("irons_nouveau_lease");
                h.assertTrue(lease.getUUID("owner").equals(caster.getUUID()) && lease.getInt("level") == 3, "Native entity retains owner and level");
                entity.discard();
            }
        }
        caster.discard(); h.succeed();
    }
    @GameTest(template = "empty", batch = "additional_cloud", timeoutTicks = 30)
    public static void delayedPoisonChildrenInheritLeaseAndAccount(GameTestHelper h) {
        var caster = cow(h, 1, 1); var mana = new AtomicInteger(100000);
        cast(h, caster, "poison_splash", at(h, 6, 2, 6), mana, 3);
        var splash = h.getLevel().getEntitiesOfClass(PoisonSplash.class, caster.getBoundingBox().inflate(15)).getFirst();
        splash.createPoisonCloud(); splash.discard();
        var cloud = h.getLevel().getEntitiesOfClass(PoisonCloud.class, caster.getBoundingBox().inflate(15)).getFirst();
        var lease = cloud.getPersistentData().getCompound("irons_nouveau_lease");
        h.assertTrue(lease.getUUID("owner").equals(caster.getUUID()) && lease.getInt("level") == 3, "Delayed child inherits original source and level");
        mana.set(0); h.assertTrue(!EffectResources.pulse(cloud, () -> true), "Delayed child shares empty account");
        int cost = SpellRegistry.getSpell(ResourceLocation.parse("irons_spellbooks:poison_splash")).getManaCost(3);
        mana.set(cost); h.assertTrue(EffectResources.pulse(cloud, () -> true) && mana.get() == 0, "Delayed child charges inherited account after refill");
        caster.discard();
        h.runAtTickTime(3, () -> { h.assertTrue(cloud.isRemoved(), "Child cleans up on owner loss"); h.succeed(); });
    }
    @GameTest(template = "empty", batch = "additional_tomb", timeoutTicks = 30)
    public static void iceTombHealingStopsWithoutMana(GameTestHelper h) {
        var caster = cow(h, 1, 1); var target = cow(h, 5, 5); target.setHealth(1);
        var mana = new AtomicInteger(100000); cast(h, caster, "ice_tomb", new EntityHitResult(target), mana, 3);
        h.assertTrue(target.getVehicle() instanceof IceTombEntity, "Target enters its own tomb");
        var tomb = (IceTombEntity)target.getVehicle();
        h.assertTrue(tomb.getOwner() == caster, "Tomb owner remains original caster");
        mana.set(0); tomb.doPositiveEffects(target); h.assertTrue(target.getHealth() == 1, "No free healing");
        int cost = SpellRegistry.getSpell(ResourceLocation.parse("irons_spellbooks:ice_tomb")).getManaCost(3);
        mana.set(cost); tomb.doPositiveEffects(target); h.assertTrue(target.getHealth() > 1 && mana.get() == 0, "Native healing resumes with payment");
        caster.discard();
        h.runAtTickTime(3, () -> { h.assertTrue(tomb.isRemoved() && !target.isPassenger(), "Cleanup releases the passenger"); target.discard(); h.succeed(); });
    }
    @GameTest(template = "empty", batch = "additional_hits", timeoutTicks = 35)
    public static void newFormsRetainDamageAndArsContinuation(GameTestHelper h) {
        var victims = new ArrayList<LivingEntity>();
        for (int i = 0; i < 2; i++) {
            String id = i == 0 ? "blood_slash" : "wither_skull";
            var caster = cow(h, 1, 2 + i * 5); caster.setHealth(500); var target = cow(h, 5, 2 + i * 5); victims.add(target);
            var form = IronsNouveau.forms().stream().filter(f -> f.spellId().getPath().equals(id)).findFirst().orElseThrow();
            var spell = new Spell(MethodProjectile.INSTANCE, form, AugmentAmplify.INSTANCE, EffectLight.INSTANCE);
            h.assertTrue(resolver(h, caster, spell, new AtomicInteger(100000)).onCast(ItemStack.EMPTY, h.getLevel()), id + " casts");
            if (i == 1) {
                var skull = h.getLevel().getEntitiesOfClass(WitherSkullProjectile.class, caster.getBoundingBox().inflate(6)).getFirst();
                var plan = ((NativeCastCarrier)skull).ironsNouveau$session().plan();
                h.assertTrue(Math.abs(skull.getDeltaMovement().length() - .64 * plan.modifiers().speedMultiplier()) < .001,
                        "Final native level and Ars acceleration determine skull launch velocity: " + skull.getDeltaMovement().length());
            }
        }
        h.succeedWhen(() -> {
            for (var victim : victims) h.assertTrue(victim.getHealth() < 1000 && victim.hasEffect(MobEffects.GLOWING), "Native hit damages and continues Ars effects");
        });
    }
    @GameTest(template = "empty", batch = "additional_targets")
    public static void targetingAndFailureBoundaries(GameTestHelper h) {
        var caster = cow(h, 1, 1); var target = cow(h, 5, 5); var mana = new AtomicInteger(100000);
        int before = mana.get(); cast(h, caster, "sacrifice", new EntityHitResult(target), mana, 1);
        h.assertTrue(target.isAlive() && mana.get() == before, "Cannot sacrifice an ordinary creature and charge mana");
        before = mana.get(); cast(h, caster, "summon_ender_chest", new EntityHitResult(target), mana, 1);
        h.assertTrue(mana.get() == before, "Nonplayer has no ender chest and is not charged");
        var mage = h.spawn(io.redspace.ironsspellbooks.registries.EntityRegistry.PYROMANCER.get(), 9, 2, 9); mage.setNoAi(true);
        cast(h, caster, "charge", new EntityHitResult(mage), mana, 1);
        cast(h, caster, "counterspell", new EntityHitResult(mage), mana, 1);
        h.assertTrue(!mage.hasEffect(MobEffectRegistry.CHARGED), "Counterspell uses Ars target and native caster eligibility");
        mage.discard();
        cast(h, caster, "acupuncture", new EntityHitResult(target), mana, 3);
        var needles = h.getLevel().getEntitiesOfClass(io.redspace.ironsspellbooks.entity.spells.blood_needle.BloodNeedle.class, target.getBoundingBox().inflate(5));
        h.assertTrue(needles.size() == 7, "Level-three acupuncture creates seven native needles");
        mana.set(0); float health = target.getHealth();
        ((dev.ironsnouveau.mixin.NativeProjectileHitAccess)needles.getFirst()).ironsNouveau$hit(new EntityHitResult(target));
        h.assertTrue(target.getHealth() < health && mana.get() == 0, "Already-paid needle impact does not charge the full cast again");
        caster.discard(); target.discard(); h.succeed();
    }
}
