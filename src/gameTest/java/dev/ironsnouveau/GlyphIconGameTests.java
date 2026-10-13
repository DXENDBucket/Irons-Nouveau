package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
@GameTestHolder("irons_nouveau_icons") @PrefixGameTestTemplate(false)
public final class GlyphIconGameTests {
    @GameTest(template="empty",timeoutTicks=20)
    public static void nativeIconDeclarations(GameTestHelper h){GlyphIconChecks.nativeIconsAreDeclaredWithoutClientClasses(h);}
}
