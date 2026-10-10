package dev.ironsnouveau;
import net.minecraft.gametest.framework.*;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau_interaction") @PrefixGameTestTemplate(false)
public final class InteractionGameTests {
    @GameTest(template="empty", timeoutTicks=70)
    public static void pointsAndEntry(GameTestHelper h) { InteractionChecks.pointsAndActualArsEntry(h, (actor, spell) -> new com.hollingsworth.arsnouveau.api.spell.SpellCaster(new net.minecraft.nbt.CompoundTag()).castSpell(h.getLevel(), actor, net.minecraft.world.InteractionHand.MAIN_HAND, null, spell)); }
    @GameTest(template="empty", timeoutTicks=70)
    public static void timeoutAndCancel(GameTestHelper h) { InteractionChecks.timeoutCompletesAndCancelDiscardsRemainder(h, (actor, spell) -> new com.hollingsworth.arsnouveau.api.spell.SpellCaster(new net.minecraft.nbt.CompoundTag()).castSpell(h.getLevel(), actor, net.minecraft.world.InteractionHand.MAIN_HAND, null, spell)); }
    @GameTest(template="empty", timeoutTicks=70)
    public static void sourceChange(GameTestHelper h) { InteractionChecks.sourceChangeNeverFinishesOldWall(h); }
    @GameTest(template="empty", timeoutTicks=70)
    public static void nativeCooldown(GameTestHelper h) { InteractionChecks.nativeCooldownCommitsAtCompletion(h, ForgeSmokeTests.player(h)); }
}
