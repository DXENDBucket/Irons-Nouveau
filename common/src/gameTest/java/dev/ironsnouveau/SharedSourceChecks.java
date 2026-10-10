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
