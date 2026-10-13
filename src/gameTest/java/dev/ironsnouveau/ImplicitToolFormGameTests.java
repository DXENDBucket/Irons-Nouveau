package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.item.ICasterTool;
import com.hollingsworth.arsnouveau.api.spell.Spell;
import com.hollingsworth.arsnouveau.setup.registry.ItemsRegistry;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder("irons_nouveau_tool_forms") @PrefixGameTestTemplate(false)
public final class ImplicitToolFormGameTests {
    @GameTest(template="empty", timeoutTicks=30)
    public static void nativeInscriptionTemplates(GameTestHelper h) {
        var caster = h.spawn(EntityType.COW, 1, 2, 1);
        caster.setNoAi(true); caster.setNoGravity(true);
        ImplicitToolFormCases.inscriptionAndCasting(h,
                spell -> inscribe(ItemsRegistry.WAND.get(), spell),
                spell -> inscribe(ItemsRegistry.ENCHANTERS_SWORD.get(), spell), caster);
    }
    private static Spell inscribe(Item item, Spell spell) {
        var stack = new ItemStack(item);
        var tool = (ICasterTool)item;
        if (!tool.isScribedSpellValid(null, null, InteractionHand.MAIN_HAND, stack, spell)) throw new AssertionError("Native tool refused template");
        var mutable = spell.mutable();
        tool.scribeModifiedSpell(null, null, InteractionHand.MAIN_HAND, stack, mutable);
        return mutable.immutable();
    }
}
