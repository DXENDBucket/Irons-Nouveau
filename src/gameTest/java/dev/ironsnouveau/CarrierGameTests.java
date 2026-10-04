package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.entity.EntityOrbitProjectile;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectOrbit;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.spell.validation.StandardSpellValidator;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.glyph.NativeFormAugment;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(IronsNouveau.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CarrierGameTests {
    private static NativeFormAugment form() { return IronsNouveau.forms().get(2); }
    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        var entity = h.spawn(EntityType.COW, x, 2, z);
        entity.setNoAi(true); entity.setNoGravity(true); entity.setYRot(-90); entity.setXRot(0);
        return entity;
    }
    private static AtomicInteger cast(GameTestHelper h, LivingEntity caster, Spell spell) {
        h.assertTrue(new StandardSpellValidator(false).validate(spell.unsafeList()).isEmpty(), "Combination valid in editor");
        var paid = new AtomicInteger();
        var source = new LivingCaster(caster) {
            @Override public boolean enoughMana(int amount) { return true; }
            @Override public void expendMana(int amount) { paid.addAndGet(amount); }
        };
        var resolver = new SpellResolver(new SpellContext(h.getLevel(), spell, caster, source, ItemStack.EMPTY));
        h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "Carrier cast succeeds");
        return paid;
    }
    private static List<AbstractMagicProjectile> shots(GameTestHelper h, LivingEntity caster) {
        return h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, caster.getBoundingBox().inflate(20), p -> p.getOwner() == caster);
    }
    private static ArsTrajectoryExecution driver(AbstractMagicProjectile shot) {
        return (ArsTrajectoryExecution)((NativeCastCarrier)shot).ironsNouveau$session().execution();
    }
    @GameTest(template = "empty", batch = "carrier_features", timeoutTicks = 30)
    public static void orbitPreservesMovementCountAndExtendedLifetime(GameTestHelper h) {
        var caster = cow(h, 3, 3);
        cast(h, caster, new Spell(MethodSelf.INSTANCE, EffectOrbit.INSTANCE, form(), AugmentSplit.INSTANCE,
                AugmentAOE.INSTANCE, AugmentExtendTime.INSTANCE, AugmentAmplify.INSTANCE));
        var shots = shots(h, caster);
        h.assertTrue(shots.size() == 4, "Orbit split count survives conversion");
        for (var shot : shots) {
            h.assertTrue(((NativeCastCarrier)shot).ironsNouveau$arsDriven(), "Motion delegated to Ars");
            h.assertTrue(driver(shot).trajectory() instanceof EntityOrbitProjectile, "Original orbit controller retained");
            h.assertTrue(((NativeCastCarrier)shot).ironsNouveau$session().plan().spellLevel() == 2, "Orbit amplification raises native level");
            h.assertTrue(((NativeCastCarrier)shot).ironsNouveau$session().plan().maxTicks() > 1200, "Ars extended lifespan retained");
            shot.tickCount = 400;
        }
        caster.setPos(caster.position().add(1, 0, 0));
        h.runAtTickTime(6, () -> {
            for (var shot : shots) {
                h.assertTrue(!shot.isRemoved(), "No native 300 tick expiry for orbit");
                var orbit = (EntityOrbitProjectile)driver(shot).trajectory();
                h.assertTrue(orbit.position().distanceTo(shot.position()) < .01, "Native entity follows Ars position");
                double dx = shot.getX() - caster.getX(), dz = shot.getZ() - caster.getZ();
                h.assertTrue(Math.abs(Math.sqrt(dx * dx + dz * dz) - 2) < .05, "Radius and moving center preserved");
            }
            h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, caster.getBoundingBox().inflate(6)).isEmpty(), "No duplicate world carrier");
            h.succeed();
        });
    }
    @GameTest(template = "empty", batch = "carrier_features", timeoutTicks = 40)
    public static void optionalArcActuallyBounces(GameTestHelper h) {
        if (CarrierProfiles.ARC.glyph() == null) { h.succeed(); return; }
        var caster = cow(h, 1, 2); caster.setXRot(55);
        for (int x = 0; x < 8; x++) for (int z = 0; z < 6; z++) h.setBlock(new BlockPos(x, 1, z), Blocks.STONE);
        cast(h, caster, new Spell(CarrierProfiles.ARC.glyph(), form(), AugmentPierce.INSTANCE, AugmentPierce.INSTANCE));
        var shot = shots(h, caster).getFirst();
        var trajectory = driver(shot).trajectory();
        h.succeedWhen(() -> {
            h.assertTrue(!shot.isRemoved(), "Bouncing shot survives impact");
            h.assertTrue(trajectory.getDeltaMovement().y > 0 && trajectory.pierceLeft < 2, "Original gravity and bounce budget operate");
        });
    }
    @GameTest(template = "empty", batch = "carrier_features", timeoutTicks = 40)
    public static void optionalHomingRetainsTargetSelection(GameTestHelper h) {
        if (CarrierProfiles.HOMING.glyph() == null) { h.succeed(); return; }
        var caster = cow(h, 1, 1); cow(h, 3, 4);
        cast(h, caster, new Spell(CarrierProfiles.HOMING.glyph(), form()));
        var shot = shots(h, caster).getFirst();
        h.succeedWhen(() -> h.assertTrue(driver(shot).trajectory().getDeltaMovement().z > .04, "Homing steers toward off-axis target"));
    }
    @GameTest(template = "empty", batch = "carrier_splash", timeoutTicks = 45)
    public static void optionalSplashAffectsMultipleTargetsOnce(GameTestHelper h) {
        if (CarrierProfiles.SPLASH.glyph() == null) { h.succeed(); return; }
        var caster = cow(h, 1, 1);
        var a = cow(h, 6, 1); var b = cow(h, 6, 4);
        var fortify = IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals("fortify")).findFirst().orElseThrow();
        var paid = cast(h, caster, new Spell(CarrierProfiles.SPLASH.glyph(), form(), AugmentAOE.INSTANCE,
                AugmentDurationDown.INSTANCE, AugmentDurationDown.INSTANCE, AugmentDurationDown.INSTANCE,
                AugmentDurationDown.INSTANCE, fortify));
        int originalCost = paid.get();
        h.succeedWhen(() -> {
            h.assertTrue(a.hasEffect(MobEffectRegistry.FORTIFY) && b.hasEffect(MobEffectRegistry.FORTIFY), "Splash resumes effects for all targets");
            h.assertTrue(a.getHealth() < a.getMaxHealth() && b.getHealth() < b.getMaxHealth(), "Native payload applies to both targets");
            h.assertTrue(paid.get() == originalCost + 2 * io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(fortify.definition().spellId()).getManaCost(1), "Each resumed effect pays once");
        });
    }
}
