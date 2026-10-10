package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectLight;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import dev.ironsnouveau.bridge.Resolution;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.api.LocationSpellAdapter;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.*;
import io.redspace.ironsspellbooks.entity.spells.fireball.SmallMagicFireball;
import io.redspace.ironsspellbooks.entity.spells.ball_lightning.BallLightning;
import io.redspace.ironsspellbooks.entity.spells.small_magic_arrow.SmallMagicArrow;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@GameTestHolder("irons_nouveau_emitters")
@PrefixGameTestTemplate(false)
public final class EmitterMotionGameTests {
    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        var cow = h.spawn(EntityType.COW, x, 2, z); cow.setNoAi(true); cow.setNoGravity(true); cow.setYRot(-90);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000); cow.setHealth(1000);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); return cow;
    }
    private static BridgeGlyph glyph(String id) { return IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow(); }
    private static int cost(String id) { return SpellRegistry.getSpell(ResourceLocation.parse("irons_spellbooks:" + id)).getManaCost(1); }
    private static SpellResolver resolver(GameTestHelper h, LivingEntity caster, Spell spell, AtomicInteger mana) {
        return new SpellResolver(new SpellContext(h.getLevel(), spell, caster, new LivingCaster(caster) {
            @Override public boolean enoughMana(int amount) { return mana.get() >= amount; }
            @Override public void expendMana(int amount) { mana.addAndGet(-amount); }
        }, ItemStack.EMPTY));
    }
    private static void cast(GameTestHelper h, LivingEntity caster, String id, HitResult hit, AtomicInteger mana) {
        var glyph = glyph(id); var resolver = resolver(h, caster, new Spell(MethodProjectile.INSTANCE, glyph), mana);
        glyph.onResolve(hit, h.getLevel(), caster, new SpellStats.Builder().build(), resolver.spellContext, resolver);
    }
    private static BlockHitResult at(GameTestHelper h, int x, int y, int z) {
        var pos = h.absolutePos(new BlockPos(x, y, z)); return new BlockHitResult(Vec3.atBottomCenterOf(pos), Direction.UP, pos.below(), false);
    }
    private static List<Entity> leased(GameTestHelper h, LivingEntity caster, String id) {
        return h.getLevel().getEntities(caster, caster.getBoundingBox().inflate(40), e -> {
            var tag = e.getPersistentData().getCompound("irons_nouveau_lease");
            return tag.hasUUID("owner") && tag.getUUID("owner").equals(caster.getUUID()) && tag.getString("spell").equals("irons_spellbooks:" + id);
        });
    }
    @GameTest(template = "empty", batch = "motion_selection")
    public static void movementUsesRecipientAndNativeDamageKeepsCaster(GameTestHelper h) {
        var caster = cow(h, 1, 1); var actor = cow(h, 6, 6); var victim = cow(h, 6, 6);
        var mana = new AtomicInteger(100000);
        for (String id : List.of("shadow_slash", "burning_dash", "ascension", "volt_strike")) {
            int before = mana.get(); cast(h, caster, id, at(h, 8, 2, 8), mana);
            h.assertTrue(mana.get() == before && caster.getDeltaMovement().lengthSqr() == 0, "Block hit cannot run " + id);
        }
        cast(h, caster, "burning_dash", new EntityHitResult(actor), mana);
        h.assertTrue(actor.getDeltaMovement().x > 0 && caster.getDeltaMovement().lengthSqr() == 0, "Only selected recipient moves in its own facing");
        var dash = actor.getEffect(MobEffectRegistry.BURNING_DASH);
        h.assertTrue(dash != null, "Native collision effect installed");
        actor.setDeltaMovement(Vec3.ZERO); victim.invulnerableTime = 0;
        MobEffectRegistry.BURNING_DASH.value().applyEffectTick(actor, dash.getAmplifier());
        h.assertTrue(victim.getHealth() < 1000 && victim.getLastHurtByMob() == caster, "Native dash damage credits original caster");
        actor.removeEffect(MobEffectRegistry.BURNING_DASH);
        victim.setHealth(1000); victim.invulnerableTime = 0;
        cast(h, caster, "volt_strike", new EntityHitResult(actor), mana);
        var volt = actor.getEffect(MobEffectRegistry.VOLT_STRIKE);
        MobEffectRegistry.VOLT_STRIKE.value().applyEffectTick(actor, volt.getAmplifier());
        h.assertTrue(victim.getHealth() < 1000 && victim.getLastHurtByMob() == caster, "Volt collision retains original caster");
        actor.removeEffect(MobEffectRegistry.VOLT_STRIKE); actor.setDeltaMovement(Vec3.ZERO);
        cast(h, caster, "shadow_slash", new EntityHitResult(actor), mana);
        h.assertTrue(actor.getDeltaMovement().lengthSqr() > 0, "Shadow Slash moves recipient");
        cast(h, caster, "ascension", new EntityHitResult(caster), mana);
        h.assertTrue(caster.getDeltaMovement().y > 0 && caster.hasEffect(MobEffectRegistry.ASCENSION), "Self recipient ascends");
        caster.discard(); actor.discard(); victim.discard(); h.succeed();
    }
    @GameTest(template = "empty", batch = "emitter_rows", timeoutTicks = 60)
    public static void nativeEmittersPayEachWaveAndLeaseChildren(GameTestHelper h) {
        var caster = cow(h, 1, 1); var mana = new AtomicInteger(cost("arrow_volley") * 2);
        cast(h, caster, "arrow_volley", at(h, 8, 2, 8), mana);
        var volley = (ArrowVolleyEntity)leased(h, caster, "arrow_volley").getFirst();
        volley.tickCount = 5; volley.tick();
        var first = leased(h, caster, "arrow_volley").stream().filter(e -> e instanceof SmallMagicArrow).toList();
        h.assertTrue(first.size() == 5 && mana.get() == cost("arrow_volley"), "First row paid by glyph activation");
        h.assertTrue(first.stream().allMatch(e -> e.getPersistentData().getCompound("irons_nouveau_lease").getBoolean("one_shot")), "Arrow impacts are prepaid and attributed");
        h.assertTrue(first.getFirst().getDeltaMovement().y < 0, "Arrow row points down toward selected location");
        first.forEach(Entity::discard); volley.tickCount = 10; volley.tick();
        h.assertTrue(mana.get() == 0 && leased(h, caster, "arrow_volley").size() == 6, "Second row pays native fee");
        leased(h, caster, "arrow_volley").stream().filter(e -> e != volley).forEach(Entity::discard);
        volley.tickCount = 15; volley.tick();
        h.assertTrue(leased(h, caster, "arrow_volley").size() == 1, "No free arrows when empty");
        var fangMana = new AtomicInteger(cost("fang_swirl"));
        for (var pos : BlockPos.betweenClosed(1, 0, 1, 15, 0, 15)) h.setBlock(pos, Blocks.STONE);
        cast(h, caster, "fang_swirl", at(h, 8, 1, 8), fangMana);
        var swirl = (FangSwirlEntity)leased(h, caster, "fang_swirl").getFirst();
        swirl.tickCount = 6; swirl.tick();
        h.assertTrue(leased(h, caster, "fang_swirl").stream().anyMatch(e -> e instanceof ExtendedEvokerFang), "Native swirl creates managed fangs");
        caster.discard();
        h.runAtTickTime(3, () -> { h.assertTrue(volley.isRemoved() && swirl.isRemoved(), "Emitters clean up on owner loss"); h.succeed(); });
    }
    @GameTest(template = "empty", batch = "timed_emission", timeoutTicks = 60)
    public static void timedEffectsStopWhenAccountIsEmpty(GameTestHelper h) {
        var caster = cow(h, 1, 1); var mana = new AtomicInteger(cost("blaze_storm") * 2);
        var before = caster.position(); cast(h, caster, "blaze_storm", at(h, 5, 4, 5), mana);
        h.assertTrue(leased(h, caster, "blaze_storm").size() == 1, "First fireball exists immediately");
        h.runAtTickTime(8, () -> {
            h.assertTrue(mana.get() == 0, "Timed second shot paid shared account");
            h.assertTrue(leased(h, caster, "blaze_storm").stream().allMatch(e -> e.getPersistentData().getCompound("irons_nouveau_lease").getBoolean("one_shot")), "Timed children do not pay twice");
        });
        h.runAtTickTime(15, () -> mana.set(cost("blaze_storm")));
        h.runAtTickTime(25, () -> {
            h.assertTrue(mana.get() == cost("blaze_storm"), "Stopped emission does not restart after refill");
            cast(h, caster, "starfall", at(h, 7, 2, 7), new AtomicInteger(cost("starfall")));
            h.assertTrue(leased(h, caster, "starfall").size() == 2, "Starfall emits two native comets");
            caster.setHealth(500);
            cast(h, caster, "cloud_of_regeneration", new EntityHitResult(caster), new AtomicInteger(cost("cloud_of_regeneration")));
            h.assertTrue(caster.getHealth() > 500, "Cloud heals its self recipient");
            caster.discard(); h.succeed();
        });
    }
    @GameTest(template = "empty", batch = "terrain_native")
    public static void terrainEntitiesAndProtectedMiningUseNativeEntryPoints(GameTestHelper h) {
        var caster = cow(h, 1, 1);
        for (var pos : BlockPos.betweenClosed(0, 0, 0, 15, 0, 15)) h.setBlock(pos, Blocks.STONE);
        for (String id : List.of("firecracker", "raise_hell", "ice_spikes", "chain_creeper")) {
            var mana = new AtomicInteger(100000); cast(h, caster, id, at(h, 6, 1, 6), mana);
            h.assertTrue(!leased(h, caster, id).isEmpty() && mana.get() < 100000, id + " creates native managed entities");
            leased(h, caster, id).forEach(Entity::discard);
        }
        var victim = cow(h, 9, 6);
        cast(h, caster, "flaming_strike", new EntityHitResult(victim), new AtomicInteger(100000));
        h.assertTrue(victim.getHealth() < 1000 && victim.getLastHurtByMob() == caster, "Flaming Strike damages at selected location");
        var player = ScrollProgressGameTests.player(h); player.setGameMode(GameType.CREATIVE);
        var pos = h.absolutePos(new BlockPos(4, 1, 4)); h.setBlock(new BlockPos(4, 1, 4), Blocks.STONE);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        Consumer<BlockEvent.BreakEvent> deny = event -> { if (event.getPos().equals(pos)) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(deny);
        try { cast(h, player, "touch_dig", hit, new AtomicInteger(0)); h.assertTrue(!h.getLevel().getBlockState(pos).isAir(), "Break event denies mining"); }
        finally { NeoForge.EVENT_BUS.unregister(deny); }
        cast(h, player, "touch_dig", hit, new AtomicInteger(0));
        h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "Allowed native dig works with creative zero mana");
        h.setBlock(new BlockPos(4, 1, 4), Blocks.STONE); player.setGameMode(GameType.ADVENTURE);
        var g = glyph("touch_dig"); var context = new Resolution(h.getLevel(), player, SpellRegistry.getSpell(g.definition().spellId()), g.definition(), new SpellStats.Builder().build());
        h.assertTrue(!((LocationSpellAdapter)g.definition().adapter()).applyAt(context, hit), "Adventure restrictions respected before mining");
        caster.discard(); victim.discard(); player.discard(); h.succeed();
    }
    @GameTest(template = "empty", batch = "new_forms", timeoutTicks = 50)
    public static void newProjectileFormsContinueArsEffects(GameTestHelper h) {
        h.assertTrue(IronsNouveau.forms().size() == 19 && IronsNouveau.glyphs().size() == 84, "All 103 glyphs register");
        var victims = new ArrayList<LivingEntity>(); var casters = new ArrayList<LivingEntity>();
        for (int i = 0; i < 2; i++) {
            String id = i == 0 ? "ball_lightning" : "flaming_barrage";
            var caster = cow(h, 1, 2 + i * 5); casters.add(caster); var target = cow(h, 5, 2 + i * 5); victims.add(target);
            var form = IronsNouveau.forms().stream().filter(f -> f.spellId().getPath().equals(id)).findFirst().orElseThrow();
            var spell = new Spell(MethodProjectile.INSTANCE, form, AugmentAmplify.INSTANCE, EffectLight.INSTANCE);
            h.assertTrue(resolver(h, caster, spell, new AtomicInteger(100000)).onCast(ItemStack.EMPTY, h.getLevel()), id + " casts");
        }
        h.succeedWhen(() -> {
            for (var victim : victims) h.assertTrue(victim.getHealth() < 1000 && victim.hasEffect(MobEffects.GLOWING), "Native hit damages and continues Ars effects");
            victims.forEach(Entity::discard); casters.forEach(Entity::discard);
        });
    }
    @GameTest(template = "empty", batch = "repeat_ball", timeoutTicks = 35)
    public static void ballRepeatsRequireManaAndHammerRespectsDeniedBlocks(GameTestHelper h) {
        var caster = cow(h, 1, 1); var target = cow(h, 8, 8);
        var form = IronsNouveau.forms().stream().filter(f -> f.spellId().getPath().equals("ball_lightning")).findFirst().orElseThrow();
        var mana = new AtomicInteger(100000);
        h.assertTrue(resolver(h, caster, new Spell(MethodProjectile.INSTANCE, form), mana).onCast(ItemStack.EMPTY, h.getLevel()), "Ars launch succeeds");
        var balls = h.getLevel().getEntitiesOfClass(BallLightning.class, caster.getBoundingBox().inflate(5));
        h.assertTrue(!balls.isEmpty(), "Native ball exists after paid Ars launch");
        var ball = balls.getFirst(); mana.set(0);
        ball.setPos(h.absoluteVec(new Vec3(3, 5, 3))); ball.setDeltaMovement(Vec3.ZERO);
        var hitAccess = (dev.ironsnouveau.mixin.NativeProjectileHitAccess)ball;
        hitAccess.ironsNouveau$hit(new EntityHitResult(target));
        float afterFirst = target.getHealth();
        h.assertTrue(afterFirst < 1000 && mana.get() == 0, "Ball first impact already paid at launch");
        h.runAtTickTime(12, () -> {
            target.invulnerableTime = 0;
            hitAccess.ironsNouveau$hit(new EntityHitResult(target));
            h.assertTrue(target.getHealth() == afterFirst, "Later ball damage requires another fee");
            mana.set(cost("ball_lightning"));
            hitAccess.ironsNouveau$hit(new EntityHitResult(target));
            h.assertTrue(target.getHealth() < afterFirst && mana.get() == 0, "Paid ball damage resumes");
            ball.discard(); caster.discard(); target.discard();
            var player = ScrollProgressGameTests.player(h); player.setGameMode(GameType.CREATIVE);
            var pos = h.absolutePos(new BlockPos(6, 1, 6)); h.setBlock(new BlockPos(6, 1, 6), Blocks.STONE);
            var hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
            cast(h, player, "spectral_hammer", hit, new AtomicInteger(0));
            var hammers = h.getLevel().getEntitiesOfClass(io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer.class,
                    new AABB(pos).inflate(6));
            h.assertTrue(!hammers.isEmpty(), "Native hammer exists at selected mineable block");
            var hammer = hammers.getFirst();
            Consumer<BlockEvent.BreakEvent> deny = event -> { if (event.getPos().equals(pos)) event.setCanceled(true); };
            NeoForge.EVENT_BUS.addListener(deny);
            try {
                for (int i = 0; i < 13; i++) hammer.tick();
                h.assertTrue(!h.getLevel().getBlockState(pos).isAir(), "Native delayed hammer honors block denial");
            } finally { NeoForge.EVENT_BUS.unregister(deny); hammer.discard(); }
            cast(h, player, "spectral_hammer", hit, new AtomicInteger(0));
            var allowed = h.getLevel().getEntitiesOfClass(io.redspace.ironsspellbooks.entity.spells.spectral_hammer.SpectralHammer.class,
                    new AABB(pos).inflate(6)).getFirst();
            for (int i = 0; i < 13; i++) allowed.tick();
            h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "Native hammer mines allowed block");
            allowed.discard(); player.discard(); h.succeed();
        });
    }
}
