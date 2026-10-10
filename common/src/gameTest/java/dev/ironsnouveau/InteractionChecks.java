package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.interaction.InputSessions;
import dev.arsconflux.api.resource.ResourceAccount;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.progression.SpellProgress;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.wall_of_fire.WallOfFireEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class InteractionChecks {
    private InteractionChecks() {}
    private static BridgeGlyph glyph() {
        return IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals("wall_of_fire")).findFirst().orElseThrow();
    }
    private static Vec3 scene(GameTestHelper h) {
        Vec3 p = h.absoluteVec(new Vec3(4, 0, 4)); p = new Vec3(p.x, h.getLevel().getMaxBuildHeight() - 32, p.z);
        for (int x = -3; x <= 9; x++) for (int z = -3; z <= 9; z++) {
            var floor = BlockPos.containing(p).offset(x, -1, z); h.getLevel().setBlockAndUpdate(floor, Blocks.STONE.defaultBlockState());
            for (int y = 1; y <= 5; y++) h.getLevel().setBlockAndUpdate(floor.above(y), Blocks.AIR.defaultBlockState());
        }
        return p;
    }
    private static LivingEntity actor(GameTestHelper h) {
        var actor = h.spawn(EntityType.COW, 1, 3, 1); actor.setNoAi(true); actor.setNoGravity(true); actor.setPos(scene(h));
        actor.setInvulnerable(true);
        actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        actor.setYRot(-90); actor.setYHeadRot(-90); actor.setYBodyRot(-90); actor.setXRot(35); return actor;
    }
    private static HitResult hit(Vec3 p) { return new BlockHitResult(p, Direction.UP, BlockPos.containing(p), false); }
    private static List<WallOfFireEntity> walls(LivingEntity actor) {
        return ((net.minecraft.server.level.ServerLevel)actor.level()).getEntitiesOfClass(WallOfFireEntity.class,
                actor.getBoundingBox().inflate(48), wall -> wall.getOwner() == actor);
    }
    private record Fixture(LivingEntity actor, Spell spell, AtomicInteger balance, AtomicInteger prefix, AtomicInteger suffix) {}
    private static Fixture begin(GameTestHelper h, LivingEntity actor) {
        var prefix = new AtomicInteger(); var suffix = new AtomicInteger(); var balance = new AtomicInteger(2000);
        var recipe = new Spell(MethodTouch.INSTANCE, new Probe("before", prefix), glyph(), new Probe("after", suffix));
        var resolver = new SpellResolver(new SpellContext(h.getLevel(), recipe, actor, new LivingCaster(actor), actor.getMainHandItem()));
        var account = new ResourceAccount() {
            public Object reservationKey() { return balance; }
            public boolean canSpend(int amount) { return balance.get() >= amount; }
            public void spend(int amount) { balance.addAndGet(-amount); }
        };
        // Keep the first anchor off the fallback look ray: native walls discard paths with <= 1 segment.
        var first = hit(actor.position().add(0, 0, 2));
        CastContexts.bind(resolver.spellContext, CastContext.of(resolver.spellContext, first, account));
        resolver.onResolveEffect(h.getLevel(), first);
        h.assertTrue(prefix.get() == 1 && suffix.get() == 0 && InputSessions.waiting(actor), "First activation pauses only the remaining recipe");
        h.assertTrue(balance.get() == 2000 - SpellRegistry.getSpell(glyph().definition().spellId()).getManaCost(1), "Initial native cost is paid once");
        h.assertTrue(walls(actor).isEmpty(), "No wall exists until selection finishes");
        return new Fixture(actor, recipe, balance, prefix, suffix);
    }
    private static void clean(LivingEntity actor) { walls(actor).forEach(Entity::discard); actor.discard(); }
    public static void pointsAndActualArsEntry(GameTestHelper h, java.util.function.BiConsumer<LivingEntity, Spell> cast) {
        var actor = actor(h); var f = begin(h, actor); int paid = f.balance.get();
        h.assertTrue(InputSessions.route(actor, InteractionHand.MAIN_HAND, f.spell, hit(actor.position().add(3, 0, 0))), "Second anchor accepted");
        h.assertTrue(InputSessions.waiting(actor) && f.suffix.get() == 0, "Second anchor still waits");
        // Real Ars entry must consume the next interaction before its normal mana/validation path.
        cast.accept(actor, f.spell);
        h.assertTrue(!InputSessions.waiting(actor) && walls(actor).size() == 1, "Third input through actual Ars entry creates exactly one native wall");
        h.assertTrue(f.prefix.get() == 1 && f.suffix.get() == 1 && f.balance.get() == paid, "Resume does not replay prefix or charge selection again");
        clean(actor); h.succeed();
    }
    public static void timeoutCompletesAndCancelDiscardsRemainder(GameTestHelper h, java.util.function.BiConsumer<LivingEntity, Spell> cast) {
        var actor = actor(h); var f = begin(h, actor);
        h.runAtTickTime(39, () -> { actor.setYRot(-90); actor.setYHeadRot(-90); actor.setXRot(35); });
        h.runAtTickTime(45, () -> {
            h.assertTrue(!InputSessions.waiting(actor) && walls(actor).size() == 1 && f.suffix.get() == 1,
                    "Native timeout: waiting=" + InputSessions.waiting(actor) + ", walls=" + walls(actor).size()
                            + ", remainder=" + f.suffix.get() + ", alive=" + actor.isAlive());
            walls(actor).forEach(Entity::discard);
            var cancelled = begin(h, actor);
            actor.setShiftKeyDown(true);
            cast.accept(actor, cancelled.spell);
            actor.setShiftKeyDown(false);
            h.assertTrue(!InputSessions.waiting(actor) && walls(actor).isEmpty() && cancelled.suffix.get() == 0,
                    "Sneak input cancels without wall or continuation");
            clean(actor); h.succeed();
        });
    }
    public static void sourceChangeNeverFinishesOldWall(GameTestHelper h) {
        var actor = actor(h); var f = begin(h, actor);
        actor.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE));
        h.runAtTickTime(45, () -> {
            h.assertTrue(!InputSessions.waiting(actor) && walls(actor).isEmpty() && f.suffix.get() == 0, "Source change cancels before timeout; remainder stays discarded");
            clean(actor); h.succeed();
        });
    }
    public static void nativeCooldownCommitsAtCompletion(GameTestHelper h, ServerPlayer player) {
        player.setPos(scene(h)); player.setNoGravity(true); player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        var nativeSpell = SpellRegistry.getSpell(glyph().definition().spellId());
        var scroll = new ItemStack(io.redspace.ironsspellbooks.registries.ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(nativeSpell, 1, scroll); SpellProgress.crafted(player, scroll);
        var oldEnabled = dev.ironsnouveau.config.SpellLevelConfig.COOLDOWNS_ENABLED.get();
        var oldChant = dev.ironsnouveau.config.SpellLevelConfig.CHANTING_ENABLED.get();
        var cds = MagicData.getPlayerMagicData(player).getPlayerCooldowns();
        try {
            dev.ironsnouveau.config.SpellLevelConfig.COOLDOWNS_ENABLED.set(true);
            dev.ironsnouveau.config.SpellLevelConfig.CHANTING_ENABLED.set(false);
            // Use an actual active-cast attempt around a resolved selection to test the cooldown ledger.
            Fixture[] f = new Fixture[1];
            var recipe = new Spell(MethodTouch.INSTANCE, glyph());
            dev.ironsnouveau.casting.ActiveCooldowns.execute(recipe, player, io.redspace.ironsspellbooks.api.spells.CastSource.SPELLBOOK, () -> {
                f[0] = begin(h, player);
                var resolver = new SpellResolver(SpellContext.fromEntity(recipe, player, player.getMainHandItem()));
                dev.ironsnouveau.casting.ActiveCooldowns.dispatched(resolver, true); return true;
            }, false);
            h.assertTrue(!cds.isOnCooldown(nativeSpell), "Initial selection defers only this spell's native cooldown");
            InputSessions.route(player, InteractionHand.MAIN_HAND, f[0].spell, hit(player.position().add(3, 0, 0)));
            InputSessions.route(player, InteractionHand.MAIN_HAND, f[0].spell, hit(player.position().add(3, 0, 3)));
            h.assertTrue(cds.isOnCooldown(nativeSpell) && f[0].suffix.get() == 1, "Completion commits native cooldown and continuation");
        } finally {
            dev.ironsnouveau.config.SpellLevelConfig.COOLDOWNS_ENABLED.set(oldEnabled);
            dev.ironsnouveau.config.SpellLevelConfig.CHANTING_ENABLED.set(oldChant);
            clean(player);
        }
        h.succeed();
    }
    private static final class Probe extends AbstractEffect {
        final AtomicInteger count;
        Probe(String id, AtomicInteger count) { super(dev.ironsnouveau.platform.Locations.id("iron_interaction_test", id), id); this.count = count; }
        public boolean isEnabled() { return true; }
        public SpellTier defaultTier() { return SpellTier.ONE; }
        protected java.util.Set<com.hollingsworth.arsnouveau.api.spell.AbstractAugment> getCompatibleAugments() { return java.util.Set.of(); }
        public int getDefaultManaCost() { return 0; }
        public void onResolve(HitResult hit, Level world, LivingEntity caster, SpellStats stats, SpellContext context, SpellResolver resolver) { count.incrementAndGet(); }
    }
}
