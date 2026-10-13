package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
import java.util.ArrayList;

@GameTestHolder("irons_nouveau_tool_forms") @PrefixGameTestTemplate(false)
public final class ImplicitToolFormGameTests {
    @GameTest(template="empty", timeoutTicks=30)
    public static void nativeInscriptionTemplates(GameTestHelper h) {
        // Ars 4 reads entity mana capabilities; Ars 5 reads the wrapped caster account.
        var caster = ForgeSmokeTests.player(h);
        var form = GlyphCatalog.forms().stream().filter(f -> f.spellId().getPath().equals("firebolt")).findFirst().orElseThrow();
        dev.ironsnouveau.progression.SpellProgress.accept(caster,
                dev.ironsnouveau.progression.CraftedSpells.EMPTY.record(form.spellId(), 1));
        ImplicitToolFormCases.inscriptionAndCasting(h,
                spell -> inscribe(ItemsRegistry.WAND.get(), spell),
                spell -> inscribe(ItemsRegistry.ENCHANTERS_SWORD.get(), spell), caster);
    }
    private static Spell inscribe(Item item, Spell spell) {
        var stack = new ItemStack(item);
        var tool = (ICasterTool)item;
        var caster = tool.getSpellCaster(stack);
        if (!tool.isScribedSpellValid(caster, null, InteractionHand.MAIN_HAND, stack, spell)) throw new AssertionError("Native tool refused template");
        tool.setSpell(caster, null, InteractionHand.MAIN_HAND, stack, new Spell(new ArrayList<>(ArsSpellAccess.parts(spell))));
        return caster.getSpell();
    }
}
