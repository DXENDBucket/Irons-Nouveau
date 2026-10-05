package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectHeal;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.glyph.BridgeGlyph;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.*;
import net.minecraft.world.phys.*;
import net.neoforged.neoforge.gametest.*;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder("irons_nouveau_movement")
@PrefixGameTestTemplate(false)
public final class MovementGameTests {
    private static BridgeGlyph glyph(String id) {
        return IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow();
    }
    @GameTest(template = "empty", batch = "movement_chant", timeoutTicks = 30)
    public static void chantCancellationKeepsOtherLeaseAndNeverTouchesMobs(GameTestHelper h) {
        var player = ScrollProgressGameTests.player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STICK));
        var mob = h.spawn(EntityType.COW, 4, 2, 4); mob.setNoAi(true);
        double speed = mob.getAttributeValue(Attributes.MOVEMENT_SPEED);
        var token = UUID.randomUUID(); var mobToken = UUID.randomUUID();
        MovementRestrictions.begin(mob, mobToken, 200);
        h.assertTrue(!MovementRestrictions.active(mob) && mob.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed, "Non-player movement is untouched");
        h.assertTrue(!ActiveChanting.defer(new Spell(MethodSelf.INSTANCE, EffectHeal.INSTANCE), player, InteractionHand.MAIN_HAND, () -> {}), "Pure Ars creates no chant");
        h.assertTrue(!MovementRestrictions.active(player), "Pure Ars creates no restriction");
        h.assertTrue(ActiveChanting.defer(new Spell(MethodSelf.INSTANCE, glyph("summon_vex")), player, InteractionHand.MAIN_HAND, () -> {}), "Start bridge chant");
        h.assertTrue(MovementRestrictions.active(player), "Actual chant creates movement lease");
        MovementRestrictions.begin(player, token, 200);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIRT));
        h.runAfterDelay(3, () -> {
            h.assertTrue(!ActiveChanting.isChanting(player) && MovementRestrictions.active(player), "Cancelling chant cannot remove an overlapping breath lease");
            boolean before = SpellLevelConfig.MOVEMENT_ENABLED.get();
            try {
                SpellLevelConfig.MOVEMENT_ENABLED.set(false);
                h.assertTrue(!MovementRestrictions.active(player), "Config disables bridge movement restrictions");
            } finally { SpellLevelConfig.MOVEMENT_ENABLED.set(before); }
            MovementRestrictions.end(player, token);
            h.assertTrue(!MovementRestrictions.active(player), "Last lease ending releases player");
            MovementRestrictions.begin(player, UUID.randomUUID(), 2);
        });
        h.runAfterDelay(7, () -> {
            h.assertTrue(!MovementRestrictions.active(player), "Expired leases cannot leave a player slowed");
            player.discard(); mob.discard(); h.succeed();
        });
    }
    private static void breathe(GameTestHelper h, LivingEntity caster, HitResult hit) {
        var glyph = glyph("poison_breath"); var mana = new AtomicInteger(100000);
        var recipe = new Spell(MethodTouch.INSTANCE, glyph);
        var context = new SpellContext(h.getLevel(), recipe, caster, new LivingCaster(caster) {
            @Override public boolean enoughMana(int cost) { return mana.get() >= cost; }
            @Override public void expendMana(int cost) { mana.addAndGet(-cost); }
        }, ItemStack.EMPTY);
        glyph.onResolve(hit, h.getLevel(), caster, new SpellStats.Builder().build(), context, new SpellResolver(context));
    }
    @GameTest(template = "empty", batch = "movement_breath", timeoutTicks = 30)
    public static void remoteBreathRestrictsOnlyPlayerHostAndCleansUp(GameTestHelper h) {
        var host = ScrollProgressGameTests.player(h);
        var caster = h.spawn(EntityType.COW, 1, 2, 5); caster.setNoAi(true); caster.setNoGravity(true);
        var mobHost = h.spawn(EntityType.COW, 5, 2, 5); mobHost.setNoAi(true); mobHost.setNoGravity(true);
        double speed = mobHost.getAttributeValue(Attributes.MOVEMENT_SPEED);
        breathe(h, caster, new EntityHitResult(host)); breathe(h, caster, new EntityHitResult(host));
        breathe(h, caster, new EntityHitResult(mobHost));
        breathe(h, caster, new BlockHitResult(caster.position().add(2, 0, 0), net.minecraft.core.Direction.UP, caster.blockPosition().east(2), false));
        h.assertTrue(MovementRestrictions.active(host) && !MovementRestrictions.active(caster), "Actual player breath host is restricted, not remote caster");
        h.assertTrue(!MovementRestrictions.active(mobHost) && mobHost.getAttributeValue(Attributes.MOVEMENT_SPEED) == speed, "Breathing mob receives no speed change");
        var cones = h.getLevel().getEntitiesOfClass(AbstractConeProjectile.class, host.getBoundingBox().inflate(25), c -> c.getOwner() == caster);
        var hosted = cones.stream().filter(c -> ((ConeState)c).ironsNouveau$anchor() == host.getId()).toList();
        h.assertTrue(hosted.size() == 2 && cones.size() == 4, "All player, mob and block breath cases created");
        hosted.getFirst().discard();
        h.runAfterDelay(2, () -> {
            h.assertTrue(MovementRestrictions.active(host), "Second breath keeps restriction after first stops");
            cones.forEach(Entity::discard);
        });
        h.runAfterDelay(4, () -> {
            h.assertTrue(!MovementRestrictions.active(host), "Last actual breath ending removes restriction");
            host.discard(); caster.discard(); mobHost.discard(); h.succeed();
        });
    }
}
