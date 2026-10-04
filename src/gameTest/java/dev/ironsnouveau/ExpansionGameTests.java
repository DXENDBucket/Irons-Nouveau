package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.effect.EffectLight;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.spell.validation.StandardSpellValidator;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.glyph.NativeFormAugment;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(IronsNouveau.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ExpansionGameTests {
    private static NativeFormAugment form(String id) {
        return IronsNouveau.forms().stream().filter(f -> f.spellId().getPath().equals(id)).findFirst().orElseThrow();
    }
    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        var cow = h.spawn(EntityType.COW, x, 2, z);
        cow.setNoAi(true); cow.setNoGravity(true); cow.setYRot(-90); cow.setXRot(0);
        cow.getAttribute(Attributes.MAX_HEALTH).setBaseValue(100); cow.setHealth(100);
        cow.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
        return cow;
    }
    private static AbstractMagicProjectile cast(GameTestHelper h, LivingEntity caster, Spell spell, AtomicInteger paid) {
        h.assertTrue(new StandardSpellValidator(false).validate(spell.unsafeList()).isEmpty(), "Editor accepts combination");
        var source = new LivingCaster(caster) {
            @Override public boolean enoughMana(int amount) { return true; }
            @Override public void expendMana(int amount) { paid.addAndGet(amount); }
        };
        var resolver = new SpellResolver(new SpellContext(h.getLevel(), spell, caster, source, ItemStack.EMPTY));
        h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "Spell casts");
        return h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, caster.getBoundingBox().inflate(12), p -> p.getOwner() == caster).getFirst();
    }
    private static int procs(ArsTrajectoryExecution driver) {
        try { return driver.trajectory().getClass().getField("totalProcs").getInt(driver.trajectory()); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    @GameTest(template = "empty", batch = "expanded_native_hits", timeoutTicks = 40)
    public static void fourNewFormsHitAndPreserveNativeSemantics(GameTestHelper h) {
        var targets = new ArrayList<LivingEntity>();
        var casters = new ArrayList<LivingEntity>();
        String[] ids = {"guiding_bolt", "lightning_lance", "magic_arrow", "blood_needles"};
        for (int i = 0; i < ids.length; i++) {
            var caster = cow(h, 1, 1 + i); caster.setHealth(50); casters.add(caster);
            targets.add(cow(h, 5, 1 + i));
            cast(h, caster, new Spell(MethodProjectile.INSTANCE, form(ids[i]), EffectLight.INSTANCE), new AtomicInteger());
        }
        h.succeedWhen(() -> {
            for (int i = 0; i < targets.size(); i++) {
                var target = targets.get(i);
                h.assertTrue(target.getHealth() < 100, ids[i] + " causes damage at " + target.position());
                h.assertTrue(target.hasEffect(MobEffects.GLOWING), "Successful hit continues Ars effect");
            }
            h.assertTrue(targets.getFirst().hasEffect(MobEffectRegistry.GUIDING_BOLT), "Guiding Bolt applies native mark");
            h.assertTrue(casters.getLast().getHealth() > 50, "Blood Needle native damage source steals life");
        });
    }
    @GameTest(template = "empty", batch = "expanded_trail", timeoutTicks = 40)
    public static void optionalTrailRepeatsDamageAndContinuation(GameTestHelper h) {
        if (CarrierProfiles.TRAIL.glyph() == null) { h.succeed(); return; }
        var caster = cow(h, 1, 1); var target = cow(h, 6, 4);
        var paid = new AtomicInteger();
        var shot = cast(h, caster, new Spell(CarrierProfiles.TRAIL.glyph(), form("guiding_bolt"),
                AugmentAmplify.INSTANCE, AugmentAOE.INSTANCE, EffectLight.INSTANCE), paid);
        int cost = paid.get();
        var session = ((NativeCastCarrier)shot).ironsNouveau$session();
        var driver = (ArsTrajectoryExecution)session.execution();
        driver.trajectory().setPos(target.position().add(0, 1, -2));
        driver.trajectory().setDeltaMovement(Vec3.ZERO);
        float damage = shot.getDamage();
        h.runAtTickTime(29, () -> {
            h.assertTrue(target.getHealth() <= 100 - 2 * damage + .01, "Trail repeats: health=" + target.getHealth() + ", damage=" + damage + ", procs=" + procs(driver) + ", age=" + driver.trajectory().tickCount + ", removed=" + shot.isRemoved());
            h.assertTrue(target.hasEffect(MobEffectRegistry.GUIDING_BOLT), "Delegated payload preserves native mark");
            h.assertTrue(target.hasEffect(MobEffects.GLOWING), "Trail resumes Ars effects");
            h.assertTrue(procs(driver) == 2, "Only real targets consume the native pulse budget");
            h.assertTrue(paid.get() == cost + 2 * io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(form("guiding_bolt").spellId()).getManaCost(2), "Every pulse pays separately");
            session.finish(CastSession.EndReason.COMPLETED);
            h.succeed();
        });
    }
    @GameTest(template = "empty", batch = "expanded_trail_empty", timeoutTicks = 40)
    public static void optionalEmptyTrailDoesNotTargetItsOwnVisual(GameTestHelper h) {
        if (CarrierProfiles.TRAIL.glyph() == null) { h.succeed(); return; }
        var caster = cow(h, 1, 1);
        var shot = cast(h, caster, new Spell(CarrierProfiles.TRAIL.glyph(), form("blood_needles"),
                AugmentExtendTime.INSTANCE), new AtomicInteger());
        var session = ((NativeCastCarrier)shot).ironsNouveau$session();
        var driver = (ArsTrajectoryExecution)session.execution();
        driver.trajectory().setPos(caster.position().add(8, 1, 5));
        driver.trajectory().setDeltaMovement(Vec3.ZERO);
        h.runAtTickTime(29, () -> {
            h.assertTrue(!shot.isRemoved() && procs(driver) == 0, "Empty trail spends no target budget on proxy");
            session.finish(CastSession.EndReason.COMPLETED);
            h.succeed();
        });
    }
}
