package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.arsconflux.api.resource.SpellResourceQuotes;
import dev.ironsnouveau.casting.BoundSpellWeapons;
import dev.ironsnouveau.casting.PresetTools;
import dev.ironsnouveau.platform.BoundSpellData;
import dev.ironsnouveau.platform.Locations;
import dev.ironsnouveau.progression.SpellProgress;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.Map;

/** Checks the same finished-tool contract against both real Ars versions and storage formats. */
public final class SharedSourceChecks {
    private SharedSourceChecks() {}
    /** Exercise the no-server branch without loading Minecraft client classes on a dedicated server. */
    public static void unboundContextsAndInvalidLeases(GameTestHelper helper) {
        var caster = helper.spawn(EntityType.COW, 1, 2, 1);
        var form = IronsNouveau.forms().stream().filter(g -> g.spellId().equals(
                Locations.id("irons_spellbooks:firebolt"))).findFirst().orElseThrow();
        var glyph = IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().equals(
                Locations.id("irons_spellbooks:heal"))).findFirst().orElseThrow();
        var spell = new Spell(MethodProjectile.INSTANCE, form);
        var tool = new ItemStack(Items.BONE);
        BoundSpellWeapons.bind(tool, spell, 3);
        var unbound = new SpellContext(null, spell, null,
                new com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster(null), tool) {
            @Override public net.minecraft.world.entity.LivingEntity getUnwrappedCaster() {
                throw new AssertionError("Non-server contexts must not request Ars' fake player");
            }
        };
        helper.assertTrue(ArsSpellAccess.level(unbound) == null && ArsSpellAccess.caster(unbound) == null,
                "An unbound context stays readable without manufacturing a caster");
        helper.assertTrue(dev.arsconflux.api.context.CastContext.of(unbound, null, null).caster() == null,
                "Shared context resolution accepts an absent entity");
        PresetTools.scoped(new PresetTools.Token(caster, tool), () -> {
            PresetTools.scoped(unbound, () -> {
                helper.assertTrue(PresetTools.current() == null, "An unbound tool does not inherit ambient authorization");
                return null;
            });
            helper.assertTrue(PresetTools.current().caster() == caster, "Tool scope is restored after an unbound query");
            return null;
        });
        helper.assertTrue(form.firstCharge(unbound, java.util.List.of()) == null
                        && glyph.firstCharge(unbound, java.util.List.of()) == null
                        && !SpellResourceQuotes.estimate(unbound).complete(),
                "A missing caster produces an unknown quote instead of a crash or a free cast");
        var resolver = new com.hollingsworth.arsnouveau.api.spell.SpellResolver(unbound);
        var valid = SpellContext.fromEntity(spell, caster, tool);
        var stats = new com.hollingsworth.arsnouveau.api.spell.SpellResolver(valid).getCastStats();
        dev.arsconflux.api.projectile.VolleyBudget.begin(resolver);
        try {
            helper.assertTrue(dev.arsconflux.api.projectile.VolleyBudget.splits(stats, 4) == 4,
                    "A non-server preview does not start a resource transaction");
        } finally { dev.arsconflux.api.projectile.VolleyBudget.finish(resolver); }
        var event = new com.hollingsworth.arsnouveau.api.event.SpellCastEvent(spell, valid);
        event.context = unbound;
        dev.ironsnouveau.casting.NativeCasting.beforeCast(event);
        helper.assertTrue(!event.isCanceled(), "A non-server pre-cast callback does not reject a client preview");
        var calls = new java.util.concurrent.atomic.AtomicInteger();
        dev.arsconflux.internal.billing.VanillaTriggerBilling.resolve(
                com.hollingsworth.arsnouveau.common.spell.effect.EffectHarm.INSTANCE, resolver,
                new net.minecraft.world.phys.EntityHitResult(caster), calls::incrementAndGet);
        helper.assertTrue(calls.get() == 1, "Native callbacks pass through without client billing");
        var serverDevice = new SpellContext(helper.getLevel(), spell, null,
                new com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster(null));
        helper.assertTrue(ArsSpellAccess.caster(serverDevice) == serverDevice.getUnwrappedCaster()
                        && ArsSpellAccess.caster(serverDevice).level() == helper.getLevel(),
                "Server devices retain Ars' original fake-player behavior");
        var projectile = new net.minecraft.world.entity.projectile.Snowball(helper.getLevel(), caster);
        projectile.getPersistentData().put("irons_nouveau_lease", new net.minecraft.nbt.CompoundTag());
        helper.assertTrue(!dev.ironsnouveau.casting.EffectResources.pulse(projectile,
                        () -> { calls.incrementAndGet(); return true; }),
                "An invalid saved lease cannot query a missing owner UUID");
        dev.ironsnouveau.casting.EffectResources.emit(projectile, 20, false, calls::incrementAndGet);
        helper.assertTrue(calls.get() == 1, "Invalid leases do not emit unowned effects");
        helper.succeed();
    }

    public static void boundToolPersistenceAndScope(GameTestHelper helper) {
        var spellId = Locations.id("irons_spellbooks:heal");
        var unrelated = Locations.id("irons_spellbooks:firebolt");
        var glyph = IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().equals(spellId)).findFirst().orElseThrow();
        var source = new Spell(MethodSelf.INSTANCE, glyph);
        var item = new ItemStack(Items.BONE);
        BoundSpellWeapons.bind(item, source, Map.of(spellId, 3, unrelated, 50));
        var loaded = BoundSpellData.loadItem(BoundSpellData.saveItem(item, helper.getLevel().registryAccess()), helper.getLevel().registryAccess());
        helper.assertTrue(BoundSpellWeapons.hasBinding(loaded), "The native ItemStack save must preserve the sealed spell");
        helper.assertTrue(ArsSpellAccess.parts(BoundSpellWeapons.program(loaded)).equals(ArsSpellAccess.parts(source)), "Storage must preserve recipe order and glyph identity");
        helper.assertTrue(BoundSpellWeapons.presetLevel(loaded, spellId) == 3, "Storage must preserve the native level");
        helper.assertTrue(BoundSpellWeapons.presetLevel(loaded, unrelated) == 0, "A tool must never authorize spells absent from its recipe");
        var caster = helper.spawn(EntityType.COW, 1, 2, 1);
        var other = helper.spawn(EntityType.COW, 3, 2, 1);
        int baseline = SpellProgress.baseLevel(caster, spellId);
        int otherBaseline = SpellProgress.baseLevel(other, spellId);
        PresetTools.scoped(new PresetTools.Token(caster, loaded), () -> {
            helper.assertTrue(SpellProgress.baseLevel(caster, spellId) == 3, "The actual caster must receive the tool's level");
            helper.assertTrue(SpellProgress.baseLevel(other, spellId) == otherBaseline, "Tool authorization must not leak to another caster");
            return null;
        });
        helper.assertTrue(SpellProgress.baseLevel(caster, spellId) == baseline, "A finished tool must not permanently change personal progression");
        var effectQuote = SpellResourceQuotes.estimate(SpellContext.fromEntity(
                new Spell(MethodSelf.INSTANCE, glyph, AugmentAmplify.INSTANCE), caster, loaded));
        helper.assertTrue(effectQuote.complete() && effectQuote.entries().get(0).charge().amount()
                == io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(spellId).getManaCost(4),
                "Dry effect quotes retain the actual tool's level and written amplification");
        var form = IronsNouveau.forms().stream().filter(g -> g.spellId().equals(unrelated)).findFirst().orElseThrow();
        var projectile = new Spell(MethodProjectile.INSTANCE, AugmentAmplify.INSTANCE, form, AugmentAmplify.INSTANCE);
        var projectileTool = new ItemStack(Items.BONE);
        BoundSpellWeapons.bind(projectileTool, projectile, Map.of(unrelated, 3));
        var formQuote = SpellResourceQuotes.estimate(SpellContext.fromEntity(projectile, caster, projectileTool));
        helper.assertTrue(formQuote.complete() && formQuote.entries().get(0).charge().amount()
                == io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(unrelated).getManaCost(5),
                "Dry form quotes include sibling augments on both sides and sealed tool levels");
        helper.assertTrue(SpellProgress.baseLevel(caster, spellId) == baseline,
                "Dry quotes restore tool scope without changing progression");
        BoundSpellWeapons.clearBinding(loaded);
        helper.assertTrue(!BoundSpellWeapons.hasBinding(loaded), "Clearing the binding must remove native stored authorization");
        helper.succeed();
    }
}
