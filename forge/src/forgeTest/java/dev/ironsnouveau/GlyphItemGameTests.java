package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau_items") @PrefixGameTestTemplate(false)
public final class GlyphItemGameTests {
    @GameTest(template="empty") public static void itemModels(GameTestHelper h) {ForgeSmokeTests.player(h);GlyphItemChecks.models(h);}
    @GameTest(template="empty") public static void allScrollLevels(GameTestHelper h) {ForgeSmokeTests.player(h);GlyphItemChecks.scrolls(h,dev.ironsnouveau.recipe.SpellScrollIngredient::new);}
}
