package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import dev.arsconflux.api.glyph.ArsSpellAccess;
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
        BoundSpellWeapons.clearBinding(loaded);
        helper.assertTrue(!BoundSpellWeapons.hasBinding(loaded), "Clearing the binding must remove native stored authorization");
        helper.succeed();
    }
}
