package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("irons_nouveau_items") @PrefixGameTestTemplate(false)
public final class GlyphItemGameTests {
    @GameTest(template="empty") public static void itemModels(GameTestHelper h) {ScrollProgressGameTests.player(h);GlyphItemChecks.models(h);}
    @GameTest(template="empty") public static void allScrollLevels(GameTestHelper h) {ScrollProgressGameTests.player(h);GlyphItemChecks.scrolls(h,id->new dev.ironsnouveau.recipe.SpellScrollIngredient(id).toVanilla());}
}
