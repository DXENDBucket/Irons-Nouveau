package dev.ironsnouveau;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.glyph.BridgeGlyph;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("irons_nouveau_breath_pose")
@PrefixGameTestTemplate(false)
public final class BreathGameTests {
    private static BridgeGlyph glyph(String id) { return IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow(); }
    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        var entity = h.spawn(EntityType.COW, x, 2, z); entity.setNoAi(true); entity.setNoGravity(true); entity.setYRot(-90);
        entity.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); entity.setHealth(100);
        entity.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); return entity;
    }
    private static SpellResolver resolver(GameTestHelper h, LivingEntity caster, Spell spell, AtomicInteger mana) {
        return new SpellResolver(new SpellContext(h.getLevel(), spell, caster, new LivingCaster(caster) {
            @Override public boolean enoughMana(int amount) { return mana.get() >= amount; }
            @Override public void expendMana(int amount) { mana.addAndGet(-amount); }
        }, ItemStack.EMPTY));
    }
    private static AbstractConeProjectile cone(GameTestHelper h, LivingEntity caster) {
        return h.getLevel().getEntitiesOfClass(AbstractConeProjectile.class, caster.getBoundingBox().inflate(30), c -> c.getOwner() == caster).getFirst();
    }
    @GameTest(template = "empty", batch = "breath_host", timeoutTicks = 35)
    public static void targetOwnsPoseButCasterOwnsDamageAndMana(GameTestHelper h) {
        var caster = cow(h, 1, 1); var host = cow(h, 2, 4); var victim = cow(h, 5, 4);
        h.runAtTickTime(2, () -> {
        var glyph = glyph("poison_breath"); int cost = SpellRegistry.getSpell(glyph.definition().spellId()).getManaCost(3);
        var mana = new AtomicInteger(cost * 2); var resolver = resolver(h, caster, new Spell(MethodTouch.INSTANCE, glyph), mana);
        TriggerGeometry.write(resolver.spellContext, new Vec3(0, 0, -1));
        var ironData = io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(caster);
        var sentinel = new io.redspace.ironsspellbooks.spells.EntityCastData(caster); ironData.setAdditionalCastData(sentinel);
        glyph.onResolve(new EntityHitResult(host), h.getLevel(), caster, new SpellStats.Builder().setAugments(java.util.Collections.nCopies(2, com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify.INSTANCE)).build(), resolver.spellContext, resolver);
        var cone = cone(h, caster);
        h.assertTrue(((ConeState)cone).ironsNouveau$anchor() == host.getId() && cone.getLookAngle().x > .99, "Entity direction wins over incoming direction");
        h.assertTrue(victim.getHealth() < 100 && victim.getLastDamageSource().getEntity() == caster, "Remote credit: health=" + victim.getHealth() + ", targets=" + ((ConeState)cone).ironsNouveau$targets().size() + ", nearby=" + h.getLevel().getEntitiesOfClass(LivingEntity.class, cone.getBoundingBox().inflate(10)).size() + ", visible=" + io.redspace.ironsspellbooks.api.util.Utils.hasLineOfSight(h.getLevel(), cone, victim, true) + ", parts=" + java.util.Arrays.stream(cone.getParts()).map(e -> e.getBoundingBox().toString()).toList() + ", victim=" + victim.getBoundingBox());
        h.assertTrue(host.getHealth() == 100 && mana.get() == cost, "Host is not damaged and caster pays once");
        h.assertTrue(ironData.getAdditionalCastData() == sentinel, "Original Iron casting data untouched");
        host.setYRot(0); host.setPos(host.position().add(0, 0, .5));
        h.runAtTickTime(6, () -> h.assertTrue(cone.position().distanceTo(host.getEyePosition().add(0, -.8, 0)) < .01 && cone.getLookAngle().z > .99, "Emission follows host eye height, movement and turning"));
        h.runAtTickTime(15, () -> h.assertTrue(mana.get() == 0 && cone.isRemoved(), "Empty mana ends breath after the second paid pulse"));
        h.runAtTickTime(23, () -> { h.assertTrue(cone.isRemoved(), "Insufficient upkeep ends breath"); mana.set(cost * 10); });
        h.runAtTickTime(25, () -> { h.assertTrue(cone.isRemoved(), "Ended breath does not resume after refill"); h.succeed(); });
        });
    }
    @GameTest(template = "empty", batch = "breath_direction")
    public static void projectileDirectionIsCapturedAndFixed(GameTestHelper h) {
        var caster = cow(h, 1, 1); var glyph = glyph("fire_breath");
        var resolver = resolver(h, caster, new Spell(MethodProjectile.INSTANCE, glyph), new AtomicInteger(10000));
        var projectile = new EntityProjectileSpell(h.getLevel(), resolver);
        projectile.setDeltaMovement(0, .3, .8);
        var context = projectile.resolver().spellContext.clone();
        caster.setYRot(90);
        var hit = new BlockHitResult(caster.position().add(2, 0, 0), net.minecraft.core.Direction.UP, caster.blockPosition().east(2), false);
        glyph.onResolve(hit, h.getLevel(), caster, new SpellStats.Builder().build(), context, resolver);
        var cone = cone(h, caster);
        h.assertTrue(cone.position().distanceTo(hit.getLocation()) < .001 && ((ConeState)cone).ironsNouveau$anchor() == -1, "Block trigger stays at hit location");
        h.assertTrue(cone.getLookAngle().dot(new Vec3(0, .3, .8).normalize()) > .999, "Incoming flight direction survives context cloning and caster turning");
        cone.discard(); projectile.discard(); h.succeed();
    }
    @GameTest(template = "empty", batch = "breath_lifetime", timeoutTicks = 25)
    public static void allConeTypesExpireWithoutUsingNativeCastingState(GameTestHelper h) {
        var caster = net.neoforged.neoforge.common.util.FakePlayerFactory.get(h.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "NouveauBreathTest"));
        caster.setPos(h.absoluteVec(new Vec3(1, 2, 1))); caster.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        var mana = new AtomicInteger(0);
        for (String id : java.util.List.of("fire_breath", "poison_breath", "dragon_breath", "cone_of_cold")) {
            var glyph = glyph(id); var resolver = resolver(h, caster, new Spell(MethodSelf.INSTANCE, glyph), mana);
            var stats = new SpellStats.Builder().addDurationModifier(-2).build();
            glyph.onResolve(new EntityHitResult(caster), h.getLevel(), caster, stats, resolver.spellContext, resolver);
        }
        var cones = h.getLevel().getEntitiesOfClass(AbstractConeProjectile.class, caster.getBoundingBox().inflate(15), c -> c.getOwner() == caster);
        h.assertTrue(cones.size() == 4 && mana.get() == 0, "All four cone types work in creative at zero mana");
        h.runAtTickTime(5, () -> h.assertTrue(cones.stream().noneMatch(Entity::isRemoved), "Creative breath does not stop for zero mana"));
        h.runAtTickTime(13, () -> { h.assertTrue(cones.stream().allMatch(Entity::isRemoved), "Duration modifier expires all cone sessions"); caster.discard(); h.succeed(); });
    }
    @GameTest(template = "empty", batch = "breath_player_head", timeoutTicks = 20)
    public static void playerBreathIgnoresFeetHitsAndTracksCrouching(GameTestHelper h) {
        var player = ScrollProgressGameTests.player(h);
        player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
        h.getLevel().addNewPlayer(player);
        var caster = cow(h, 1, 6);
        var cones = new java.util.ArrayList<AbstractConeProjectile>();
        for (String id : java.util.List.of("fire_breath", "poison_breath", "dragon_breath", "cone_of_cold")) {
            var glyph = glyph(id);
            var source = id.equals("fire_breath") ? player : caster;
            var resolver = resolver(h, source, new Spell(MethodSelf.INSTANCE, glyph), new AtomicInteger(100000));
            glyph.onResolve(new EntityHitResult(player, player.position()), h.getLevel(), source,
                    new SpellStats.Builder().build(), resolver.spellContext, resolver);
        }
        cones.addAll(h.getLevel().getEntitiesOfClass(AbstractConeProjectile.class, player.getBoundingBox().inflate(15)));
        h.assertTrue(cones.size() == 4, "Self and remote casts create all four breath types");
        for (var cone : cones) {
            h.assertTrue(cone.position().distanceTo(player.getEyePosition().add(0, -.8, 0)) < .0001,
                    "Feet hit does not become a feet-level breath cone");
            var expected = player.position().add(0, player.getEyeHeight() * .9f, 0).add(player.getLookAngle().scale(
                    cone instanceof io.redspace.ironsspellbooks.entity.spells.cone_of_cold.ConeOfColdProjectile ? 1.5 : 1.6));
            h.assertTrue(BreathVisuals.particleOrigin(cone).distanceTo(expected) < .0001, "Particles start at native mouth offset");
        }
        player.setPose(Pose.CROUCHING); player.setYRot(90); player.setXRot(-30);
        // Shared client placement must use current eye height rather than the cast-time height.
        for (var cone : cones) {
            BreathVisuals.tick(cone);
            h.assertTrue(cone.position().distanceTo(player.getEyePosition().add(0, -.8, 0)) < .0001,
                    "Client placement tracks crouching");
        }
        h.runAtTickTime(3, () -> {
            for (var cone : cones) {
                h.assertTrue(cone.position().distanceTo(player.getEyePosition().add(0, -.8, 0)) < .0001,
                        "Server collision placement tracks current eyes");
                h.assertTrue(cone.getLookAngle().dot(player.getLookAngle()) > .999, "Cone follows recipient facing");
            }
            cones.forEach(Entity::discard); player.discard(); caster.discard(); h.succeed();
        });
    }
}
