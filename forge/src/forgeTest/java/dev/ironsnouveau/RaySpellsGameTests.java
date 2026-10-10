package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.glyph.BridgeGlyph;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import io.redspace.ironsspellbooks.entity.spells.electrocute.ElectrocuteProjectile;
import io.redspace.ironsspellbooks.entity.spells.ray_of_frost.RayOfFrostVisualEntity;
import io.redspace.ironsspellbooks.entity.spells.sunbeam.SunbeamEntity;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraftforge.gametest.*;


@GameTestHolder("irons_nouveau_rays")
@PrefixGameTestTemplate(false)
public final class RaySpellsGameTests {
    private static BridgeGlyph glyph(String id) {
        return IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow();
    }
    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        var cow = h.spawn(EntityType.COW, x, 2, z); cow.setNoAi(true); cow.setNoGravity(true); cow.setYRot(-90);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); cow.setHealth(100);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1); return cow;
    }
    private static LivingEntity caster(GameTestHelper h) {
        var p = ForgeSmokeTests.player(h); p.setPos(h.absoluteVec(new Vec3(1, 1.6, 1))); p.setYRot(-90); p.setXRot(0); p.setNoGravity(true);
        p.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN.get()).setBaseValue(0);
        h.getLevel().addNewPlayer(p);
        p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); p.setHealth(100); return p;
    }
    private static MagicData budget(LivingEntity caster, int amount) {
        var mana = MagicData.getPlayerMagicData(caster); mana.setMana(amount); return mana;
    }
    private static void cast(GameTestHelper h, LivingEntity caster, BridgeGlyph glyph, HitResult hit, Vec3 incoming, MagicData mana) {
        dev.ironsnouveau.progression.SpellProgress.accept((net.minecraft.world.entity.player.Player)caster, dev.ironsnouveau.progression.CraftedSpells.EMPTY.record(glyph.definition().spellId(), 1));
        var resolver = new SpellResolver(SpellContext.fromEntity(new Spell(MethodTouch.INSTANCE, glyph), caster, ItemStack.EMPTY));
        TriggerGeometry.write(resolver.spellContext, incoming);
        glyph.onResolve(hit, h.getLevel(), caster, new SpellStats.Builder().build(), resolver.spellContext, resolver);
    }
    @GameTest(template = "empty", templateNamespace = "irons_nouveau_rays", timeoutTicks = 30)
    public static void frostRemoteEyesNativeDamageFreezingAndVisualLifetime(GameTestHelper h) {
        var caster = caster(h); var host = cow(h, 4, 5); var target = cow(h, 8, 5);
        var spell = SpellRegistry.getSpell(glyph("ray_of_frost").definition().spellId());
        var mana = budget(caster, spell.getManaCost(1));
        var data = MagicData.getPlayerMagicData(caster);
        var sentinel = new io.redspace.ironsspellbooks.spells.EntityCastData(caster); data.setAdditionalCastData(sentinel);
        h.runAtTickTime(2, () -> {
            cast(h, caster, glyph("ray_of_frost"), new EntityHitResult(host, host.position()), new Vec3(0, 0, -1), mana);
            var visuals = h.getLevel().getEntitiesOfClass(RayOfFrostVisualEntity.class, host.getBoundingBox().inflate(40));
            h.assertTrue(visuals.size() == 1, "Uses the native frost entity with both native renderer layers");
            var visual = visuals.get(0);
            h.assertTrue(visual.position().distanceTo(host.getEyePosition().add(0, -.75, 0)) < .001, "Native visual offset uses host eyes, not caster or hit feet");
            h.assertTrue(visual.getLookAngle().x > .99 && visual.distance > 2 && visual.distance < 5, "Remote ray clips to the target along host facing");
            float expected = 3 + spell.getSpellPower(1, caster) * 1.5f;
            h.assertTrue(Math.abs(target.getHealth() - (100 - expected)) < .01, "Native ray damage formula is retained");
            h.assertTrue(target.getLastDamageSource().getEntity() == caster && host.getHealth() == 100, "Caster owns damage; host is excluded");
            h.assertTrue(target.getTicksFrozen() > 0, "Native freeze damage source is retained");
            h.assertTrue(mana.getMana() == 0 && data.getAdditionalCastData() == sentinel && NativeRayGeometry.current() == null,
                    "One payment, no native casting mutation or leaked geometry scope");
            h.runAtTickTime(20, () -> { h.assertTrue(visual.isRemoved(), "Native visual ends after 15 ticks without upkeep"); h.succeed(); });
        });
    }
    @GameTest(template = "empty", templateNamespace = "irons_nouveau_rays")
    public static void frostPointDirectionBlockClippingAndOriginalIronCast(GameTestHelper h) {
        var caster = caster(h); caster.setYRot(0);
        Vec3 start = h.absoluteVec(new Vec3(4, 2.5, 5));
        var wall = BlockPos.containing(start.add(3, 0, 0)); h.getLevel().setBlock(wall, Blocks.STONE.defaultBlockState(), 3);
        var behindWall = cow(h, 9, 5);
        cast(h, caster, glyph("ray_of_frost"), new BlockHitResult(start, Direction.UP, BlockPos.containing(start), false),
                new Vec3(1, 0, 0), budget(caster, 10000));
        var visual = h.getLevel().getEntitiesOfClass(RayOfFrostVisualEntity.class, new AABB(start, start).inflate(40)).get(0);
        h.assertTrue(visual.position().distanceTo(start.add(0, -.75, 0)) < .001 && visual.getLookAngle().x > .99,
                "Point trigger uses captured incoming direction and exact origin");
        h.assertTrue(Math.abs(visual.distance - 3) < .01 && behindWall.getHealth() == 100, "Block stops native frost visuals and damage");
        visual.discard();
        var target = cow(h, 1, 5);
        var spell = SpellRegistry.getSpell(glyph("ray_of_frost").definition().spellId());
        spell.onCast(h.getLevel(), 1, caster, CastSource.NONE, new MagicData());
        var nativeVisual = h.getLevel().getEntitiesOfClass(RayOfFrostVisualEntity.class, caster.getBoundingBox().inflate(40), e -> !e.isRemoved()).get(0);
        h.assertTrue(nativeVisual.getLookAngle().z > .99 && target.getHealth() < 100 && NativeRayGeometry.current() == null,
                "An ordinary Iron cast still uses its own native geometry");
        nativeVisual.discard(); h.succeed();
    }
    @GameTest(template = "empty", templateNamespace = "irons_nouveau_rays", timeoutTicks = 30)
    public static void electrocuteHostNativeHitManaAndAnimationAge(GameTestHelper h) {
        var caster = caster(h); caster.setYRot(0); var host = cow(h, 3, 5); var target = cow(h, 6, 5);
        int cost = SpellRegistry.getSpell(glyph("electrocute").definition().spellId()).getManaCost(1);
        var mana = budget(caster, cost * 2);
        h.runAtTickTime(2, () -> {
            cast(h, caster, glyph("electrocute"), new EntityHitResult(host), new Vec3(0, 0, -1), mana);
            var cone = h.getLevel().getEntitiesOfClass(ElectrocuteProjectile.class, host.getBoundingBox().inflate(30)).get(0);
            h.assertTrue(target.getHealth() < 100 && target.getLastDamageSource().getEntity() == caster, "Native electric hit and caster attribution work");
            h.assertTrue(host.getHealth() == 100 && mana.getMana() == cost && ((ConeState)cone).ironsNouveau$anchor() == host.getId(), "Host excluded, initial pulse paid exactly once");
            host.setYRot(0); host.setPos(host.position().add(0, 0, .5));
            h.runAtTickTime(6, () -> {
                h.assertTrue(cone.getAge() > 0 && cone.position().distanceTo(host.getEyePosition().add(0, -.8, 0)) < .001
                        && cone.getLookAngle().z > .99, "Native cone age advances and remote facing tracks host, not owner");
            });
            h.runAtTickTime(18, () -> { h.assertTrue(mana.getMana() == 0 && cone.isRemoved(), "Empty mana removes all native lightning geometry"); h.succeed(); });
        });
    }
    @GameTest(template = "empty", templateNamespace = "irons_nouveau_rays", timeoutTicks = 30)
    public static void sunbeamRemoteWindupNativeImpactAndOnePayment(GameTestHelper h) {
        var caster = caster(h); var target = cow(h, 8, 5);
        var spell = SpellRegistry.getSpell(glyph("sunbeam").definition().spellId());
        var mana = budget(caster, spell.getManaCost(1));
        cast(h, caster, glyph("sunbeam"), new EntityHitResult(target), Vec3.ZERO, mana);
        var beam = h.getLevel().getEntitiesOfClass(SunbeamEntity.class, target.getBoundingBox().inflate(3)).get(0);
        h.assertTrue(beam.position().distanceTo(target.position()) < .001 && beam.getOwner() == caster, "Column is placed at the Ars target like Starfall");
        h.assertTrue(mana.getMana() == 0 && target.getHealth() == 100, "Paid once, damage waits for native windup");
        h.runAtTickTime(8, () -> h.assertTrue(!beam.isRemoved() && target.getHealth() == 100, "Native warmup retained"));
        h.runAtTickTime(21, () -> {
            h.assertTrue(Math.abs(target.getHealth() - (100 - spell.getSpellPower(1, caster) * .5f)) < .01,
                    "Native delayed column hits even when the initial payment exhausted mana");
            h.assertTrue(target.getLastDamageSource().getEntity() == caster && beam.isRemoved() && mana.getMana() == 0,
                    "Native damage attribution/removal retained without double billing"); h.succeed();
        });
    }
    @GameTest(template = "empty", templateNamespace = "irons_nouveau_rays", timeoutTicks = 30)
    public static void siphonNativeDamageAndUpkeepWithVisualPacketRoundtrip(GameTestHelper h) {
        var caster = caster(h); var target = cow(h, 5, 1);
        int cost = SpellRegistry.getSpell(glyph("ray_of_siphoning").definition().spellId()).getManaCost(1);
        var mana = budget(caster, cost * 2);
        caster.setHealth(60);
        cast(h, caster, glyph("ray_of_siphoning"), new EntityHitResult(caster), Vec3.ZERO, mana);
        h.assertTrue(target.getHealth() < 100 && target.getLastDamageSource().getEntity() == caster,
                "Native siphon damage remains attributed to the caster");
        h.assertTrue(mana.getMana() == cost, "First siphon pulse paid once");
        var packet = new dev.ironsnouveau.network.SiphonRayVisualPayload(java.util.UUID.randomUUID(),
                h.getLevel().dimension().location(), caster.getId(), caster.getUUID(), caster.getUUID(),
                caster.getEyePosition(), caster.getLookAngle(), Vec3.ZERO, true, 30, 10);
        var buffer = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.buffer());
        try {
            dev.ironsnouveau.network.SiphonRayVisualPayload.encode(packet, buffer);
            h.assertTrue(packet.equals(dev.ironsnouveau.network.SiphonRayVisualPayload.decode(buffer)),
                    "Forge packet preserves ray anchors, geometry and lifetime");
        } finally { buffer.release(); }
        h.runAtTickTime(18, () -> {
            h.assertTrue(mana.getMana() == 0, "Siphon upkeep exhausts the two-pulse budget"); h.succeed();
        });
    }
}
