package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import com.hollingsworth.arsnouveau.common.spell.validation.StandardSpellValidator;
import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.glyph.NativeFormAugment;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@GameTestHolder(IronsNouveau.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BridgeGameTests {
    private static BridgeGlyph effect(String id) {
        return IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow();
    }
    private static NativeFormAugment form(String id) {
        return IronsNouveau.forms().stream().filter(g -> g.spellId().getPath().equals(id)).findFirst().orElseThrow();
    }
    private static LivingEntity cow(GameTestHelper h, int x) {
        var cow = h.spawn(EntityType.COW, x, 2, 1);
        cow.setNoAi(true); cow.setNoGravity(true); cow.setYRot(-90); cow.setXRot(0);
        return cow;
    }
    private static SpellResolver resolver(LivingEntity caster, Spell spell, AtomicInteger charged) {
        var source = new LivingCaster(caster) {
            @Override public boolean enoughMana(int amount) { return true; }
            @Override public void expendMana(int amount) { charged.addAndGet(amount); }
        };
        return new SpellResolver(new SpellContext(caster.level(), spell, caster, source, ItemStack.EMPTY));
    }
    private static List<AbstractMagicProjectile> projectiles(GameTestHelper h, LivingEntity caster) {
        return h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, caster.getBoundingBox().inflate(16), p -> p.getOwner() == caster);
    }
    @GameTest(template = "empty")
    public static void registrationAndContextualValidation(GameTestHelper h) {
        var all = new ArrayList<AbstractSpellPart>();
        all.addAll(IronsNouveau.forms()); all.addAll(IronsNouveau.glyphs());
        h.assertTrue(all.size() == 103 && IronsNouveau.forms().size() == 19, "Nineteen forms and eighty-four effects");
        for (var glyph : all) {
            h.assertTrue(BuiltInRegistries.ITEM.getKey(glyph.getGlyph()).equals(glyph.getRegistryName()), "Registered glyph");
            h.assertTrue(h.getLevel().getRecipeManager().byKey(glyph.getRegistryName()).isEmpty(), "Scroll crafting replaces standalone glyph recipes");
        }
        var validator = new StandardSpellValidator(false);
        h.assertTrue(validator.validate(List.of(MethodProjectile.INSTANCE, form("firebolt"), AugmentAmplify.INSTANCE)).isEmpty(), "Native form accepts amplify in editor");
        h.assertTrue(!validator.validate(List.of(MethodProjectile.INSTANCE, form("firebolt"), form("icicle"))).isEmpty(), "Reject two forms");
        h.assertTrue(!validator.validate(List.of(MethodTouch.INSTANCE, form("firebolt"))).isEmpty(), "Reject wrong method");
        h.assertTrue(!validator.validate(List.of(MethodProjectile.INSTANCE, form("firebolt"), AugmentPierce.INSTANCE)).isEmpty(), "No silent pierce conversion");
        int beyond = SpellRegistry.getSpell(form("firebolt").spellId()).getMaxLevel() + 8;
        h.assertTrue(new CastModifiers(beyond, 1).resolveLevel(1) == beyond + 1, "No native max-level clamp");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void nativeConversionKeepsSplitSpeedAndLateLevels(GameTestHelper h) {
        var caster = cow(h, 1);
        var charged = new AtomicInteger();
        for (var form : IronsNouveau.forms().subList(0, 7)) {
            int delta = SpellRegistry.getSpell(form.spellId()).getMaxLevel() + 2;
            var parts = new ArrayList<AbstractSpellPart>(List.of(MethodProjectile.INSTANCE, form, AugmentAccelerate.INSTANCE, AugmentSplit.INSTANCE));
            for (int i = 0; i < delta; i++) parts.add(AugmentAmplify.INSTANCE);
            var spell = new Spell(parts);
            h.assertTrue(resolver(caster, spell, charged).onCast(ItemStack.EMPTY, h.getLevel()), "Actual Ars cast succeeds");
            var shots = projectiles(h, caster);
            h.assertTrue(shots.size() == 2, "Ars split creates two native shots");
            for (var shot : shots) {
                String entityName = form.spellId().getPath().equals("blood_needles") ? "blood_needle" : form.spellId().getPath();
                h.assertTrue(BuiltInRegistries.ENTITY_TYPE.getKey(shot.getType()).getPath().contains(entityName), "Actual native projectile type");
                var session = ((NativeCastCarrier) shot).ironsNouveau$session();
                h.assertTrue(session.plan().spellLevel() == delta + 1, "Final level beyond native cap");
                float factor = switch (form.spellId().getPath()) {
                    case "blood_needles" -> .25f;
                    case "magic_arrow", "lightning_lance" -> 1f;
                    default -> .5f;
                };
                float expected = SpellRegistry.getSpell(form.spellId()).getSpellPower(delta + 1, caster) * factor;
                h.assertTrue(Math.abs(shot.getDamage() - expected) < .01, "Damage evaluated using final level");
                h.assertTrue(Math.abs(shot.getDeltaMovement().length() / shot.getSpeed() - 1.25 / .75) < .08, "Final Ars speed mapped once");
                session.finish(CastSession.EndReason.COMPLETED);
            }
            h.assertTrue(h.getLevel().getEntitiesOfClass(EntityProjectileSpell.class, caster.getBoundingBox().inflate(8)).isEmpty(), "Ars carrier never enters world");
        }
        h.assertTrue(charged.get() > 0, "Ars owns payment");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 60)
    public static void realCollisionResumesOnceWithoutSecondPayment(GameTestHelper h) {
        var caster = cow(h, 1);
        var target = cow(h, 5);
        var charged = new AtomicInteger();
        var spell = new Spell(MethodProjectile.INSTANCE, form("magic_missile"), AugmentAmplify.INSTANCE, effect("fortify"));
        h.assertTrue(resolver(caster, spell, charged).onCast(ItemStack.EMPTY, h.getLevel()), "Cast accepted");
        int cost = charged.get();
        float expected = target.getMaxHealth() - SpellRegistry.getSpell(form("magic_missile").spellId()).getSpellPower(2, caster) * .5f;
        h.succeedWhen(() -> {
            h.assertTrue(target.hasEffect(MobEffectRegistry.FORTIFY), "Native collision resumes Ars chain");
            h.assertTrue(Math.abs(target.getHealth() - expected) < .01, "One native hit only");
            h.assertTrue(charged.get() == cost + SpellRegistry.getSpell(effect("fortify").definition().spellId()).getManaCost(1), "Continuation pays its own effect once");
        });
    }
    @GameTest(template = "empty")
    public static void accessCancellationAndSessionCleanup(GameTestHelper h) {
        var caster = cow(h, 1);
        var charged = new AtomicInteger();
        var spell = new Spell(MethodProjectile.INSTANCE, form("firebolt"));
        Consumer<GlyphAccessEvent> deny = event -> { if (event.actor() == caster) event.setCanceled(true); };
        NeoForge.EVENT_BUS.addListener(deny);
        try {
            h.assertTrue(!resolver(caster, spell, charged).onCast(ItemStack.EMPTY, h.getLevel()), "Denied before execution");
            h.assertTrue(projectiles(h, caster).isEmpty() && charged.get() == 0, "Denied cast has no spawn or cost");
        } finally { NeoForge.EVENT_BUS.unregister(deny); }
        h.assertTrue(resolver(caster, spell, charged).onCast(ItemStack.EMPTY, h.getLevel()), "Allowed cast");
        var shot = projectiles(h, caster).getFirst();
        var session = ((NativeCastCarrier) shot).ironsNouveau$session();
        caster.discard(); session.tick();
        h.assertTrue(shot.isRemoved() && session.state() == CastSession.State.CANCELLED, "Lost owner cleans up execution");
        session.finish(CastSession.EndReason.EXPIRED);
        h.assertTrue(session.state() == CastSession.State.CANCELLED, "Close is idempotent");
        h.succeed();
    }
    @GameTest(template = "empty")
    public static void supportEffectsUseNativeLevelAndCost(GameTestHelper h) {
        var caster = cow(h, 1); var target = cow(h, 3);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000);
        target.setHealth(1);
        var glyph = effect("heal"); var nativeSpell = SpellRegistry.getSpell(glyph.definition().spellId());
        int level = nativeSpell.getMaxLevel() + 2;
        var charged = new AtomicInteger();
        var context = resolver(caster, new Spell(MethodTouch.INSTANCE, glyph), charged).spellContext;
        var stats = new SpellStats.Builder().setAugments(java.util.Collections.nCopies(level - 1, AugmentAmplify.INSTANCE)).setAmplification(99).build();
        Consumer<GlyphAccessEvent> verify = event -> {
            if (event.actor() == caster && event.glyphId().equals(glyph.getRegistryName()))
                h.assertTrue(event.spellLevel() == level, "Permission uses final uncapped level");
        };
        NeoForge.EVENT_BUS.addListener(verify);
        try { glyph.onResolve(new EntityHitResult(target), h.getLevel(), caster, stats, context, null); }
        finally { NeoForge.EVENT_BUS.unregister(verify); }
        h.assertTrue(Math.abs(target.getHealth() - 1 - nativeSpell.getSpellPower(level, caster)) < .01, "Native healing above original maximum, without extra Ars scalar");
        h.assertTrue(charged.get() == nativeSpell.getManaCost(level), "Effect pays its final native level");
        h.assertTrue(SpellLevels.resolve(List.of(AugmentDampen.INSTANCE, AugmentDampen.INSTANCE)) == 1, "Minimum remains one");
        h.succeed();
    }
}
