package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.progression.SpellProgress;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.api.spells.SpellRarity;
import io.redspace.ironsspellbooks.gui.arcane_anvil.ArcaneAnvilMenu;
import io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu;
import io.redspace.ironsspellbooks.item.InkItem;
import io.redspace.ironsspellbooks.registries.BlockRegistry;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.neoforged.neoforge.gametest.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@GameTestHolder(IronsNouveau.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ScrollProgressGameTests {
    static ServerPlayer player(GameTestHelper h) {
        io.redspace.ironsspellbooks.api.config.SpellConfigManager.onDatapackSync(
                new net.neoforged.neoforge.event.OnDatapackSyncEvent(h.getLevel().getServer().getPlayerList(), null));
        var cookie = CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ScrollStudent"), false);
        var player = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        // Real ServerPlayer crafting identity, without pretending a headless test performed the client mod handshake.
        player.connection = new ServerGamePacketListenerImpl(h.getLevel().getServer(), connection, player, cookie) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) {}
        };
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        player.setGameMode(GameType.SURVIVAL); return player;
    }
    static ScrollForgeMenu forge(GameTestHelper h, ServerPlayer player, SpellRarity rarity) {
        var pos = new BlockPos(3, 1, 3); h.setBlock(pos, BlockRegistry.SCROLL_FORGE_BLOCK.get());
        var menu = new ScrollForgeMenu(1, player.getInventory(), h.getBlockEntity(pos));
        player.containerMenu = menu;
        menu.getBlankScrollSlot().set(new ItemStack(Items.PAPER));
        menu.getFocusSlot().set(new ItemStack(Items.BLAZE_ROD));
        menu.getInkSlot().set(new ItemStack(InkItem.getInkForRarity(rarity)));
        menu.setRecipeSpell(SpellRegistry.FIREBOLT_SPELL.get()); return menu;
    }
    private static ItemStack scroll(int level) {
        var stack = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(SpellRegistry.FIREBOLT_SPELL.get(), level, stack); return stack;
    }
    @GameTest(template = "empty", batch = "scroll_progress")
    public static void committedCraftsOnlyAndAutomaticLearning(GameTestHelper h) {
        var player = player(h); var spell = SpellRegistry.FIREBOLT_SPELL.get(); var id = spell.getSpellResource();
        var glyph = IronsNouveau.forms().stream().filter(f -> f.spellId().equals(id)).findFirst().orElseThrow();
        player.getInventory().add(scroll(spell.getMaxLevel()));
        h.assertTrue(SpellProgress.craftedLevel(player, id) == 0 && !GlyphAccessEvent.allowed(player, glyph.getRegistryName(), id, 1, GlyphAccessEvent.Action.RESOLVE),
                "Receiving a high scroll neither unlocks nor permits the glyph");
        var menu = forge(h, player, SpellRarity.COMMON);
        h.assertTrue(!menu.getResultSlot().getItem().isEmpty() && SpellProgress.craftedLevel(player, id) == 0, "Preview is not a completed craft");
        menu.clicked(39, 0, ClickType.PICKUP, player);
        h.assertTrue(SpellProgress.craftedLevel(player, id) == 1 && CapabilityRegistry.getPlayerDataCap(player).knowsGlyph(glyph), "Normal take records level and teaches glyph");
        h.assertTrue(menu.getInkSlot().getItem().isEmpty() && menu.getBlankScrollSlot().getItem().isEmpty(), "Award follows ingredient consumption");
        var other = player(h);
        h.assertTrue(SpellProgress.craftedLevel(other, id) == 0, "History belongs only to the maker");
        player.getInventory().clearContent();
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.COBBLESTONE, 64));
        menu = forge(h, player, SpellRarity.RARE);
        int level = ISpellContainer.get(menu.getResultSlot().getItem()).getSpellAtIndex(0).getLevel();
        h.assertTrue(menu.quickMoveStack(player, 39).isEmpty() && SpellProgress.craftedLevel(player, id) == 1, "Full inventory awards no preview result");
        player.getInventory().setItem(0, ItemStack.EMPTY);
        h.assertTrue(!menu.quickMoveStack(player, 39).isEmpty() && SpellProgress.craftedLevel(player, id) == level, "Shift take records the actual high scroll despite emptied callback stack");
        menu = forge(h, player, SpellRarity.COMMON); menu.clicked(39, 0, ClickType.PICKUP, player);
        h.assertTrue(SpellProgress.craftedLevel(player, id) == level, "Later lower crafts never reduce mastery");
        player.discard(); other.discard(); h.succeed();
    }
    @GameTest(template = "empty", batch = "scroll_progress")
    public static void upgradingLootSurvivesSaveDeathAndCreative(GameTestHelper h) {
        var player = player(h); var spell = SpellRegistry.FIREBOLT_SPELL.get(); var id = spell.getSpellResource();
        var menu = new ArcaneAnvilMenu(2, player.getInventory(), ContainerLevelAccess.NULL); player.containerMenu = menu;
        menu.getSlot(0).set(scroll(2)); menu.getSlot(1).set(new ItemStack(InkItem.getInkForRarity(spell.getRarity(3))));
        h.assertTrue(!menu.getSlot(menu.getResultSlot()).getItem().isEmpty() && SpellProgress.craftedLevel(player, id) == 0, "Upgrading a found scroll must be committed");
        h.assertTrue(!menu.quickMoveStack(player, menu.getResultSlot()).isEmpty(), "Upgrade output is really taken");
        h.assertTrue(SpellProgress.craftedLevel(player, id) == 3 && menu.getSlot(1).getItem().isEmpty(), "Loot upgraded with ink becomes personal level three");
        var saved = player.serializeAttachments(h.getLevel().registryAccess());
        var restored = player(h);
        restored.setData(SpellProgress.CRAFTED, dev.ironsnouveau.progression.CraftedSpells.CODEC.parse(
                net.minecraft.nbt.NbtOps.INSTANCE, saved.get("irons_nouveau:crafted_spells")).getOrThrow());
        h.assertTrue(SpellProgress.craftedLevel(restored, id) == 3, "Serialized attachment preserves crafting history");
        var respawned = player(h); respawned.copyAttachmentsFrom(player, true);
        h.assertTrue(SpellProgress.craftedLevel(respawned, id) == 3, "Death-copy retains crafting history");
        player.setGameMode(GameType.CREATIVE);
        h.assertTrue(SpellProgress.baseLevel(player, id) == spell.getMaxLevel(), "Creative base is native maximum");
        var forge = forge(h, player, SpellRarity.LEGENDARY); forge.clicked(39, 0, ClickType.PICKUP, player);
        h.assertTrue(SpellProgress.craftedLevel(player, id) == 3, "Creative crafts do not change survival history");
        player.setGameMode(GameType.SURVIVAL);
        h.assertTrue(SpellLevels.resolve(player, id, List.of(AugmentAmplify.INSTANCE)) == 4, "Leaving creative restores earned base plus augment");
        player.discard(); restored.discard(); respawned.discard(); h.succeed();
    }
    @GameTest(template = "empty", batch = "scroll_progress")
    public static void recordedBaseDrivesConversionBudgetAndEffects(GameTestHelper h) {
        var player = player(h); var menu = forge(h, player, SpellRarity.RARE);
        int base = ISpellContainer.get(menu.getResultSlot().getItem()).getSpellAtIndex(0).getLevel();
        menu.clicked(39, 0, ClickType.PICKUP, player);
        var form = IronsNouveau.forms().stream().filter(f -> f.spellId().getPath().equals("firebolt")).findFirst().orElseThrow();
        int level = base + 1; var mana = new AtomicInteger(100000);
        // Ars 'n' Spells reserves the delivery method's fee from the real shared player pool.
        player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA).setBaseValue(100000);
        io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player).setMana(100000);
        var resolver = new SpellResolver(new SpellContext(h.getLevel(), new Spell(MethodProjectile.INSTANCE, form, AugmentAmplify.INSTANCE), player,
                new LivingCaster(player) {
                    @Override public boolean enoughMana(int cost) { return mana.get() >= cost; }
                    @Override public void expendMana(int cost) { mana.addAndGet(-cost); }
                }, ItemStack.EMPTY));
        int expected = SpellRegistry.getSpell(form.spellId()).getManaCost(level);
        h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "A personally learned high form casts");
        var shots = h.getLevel().getEntitiesOfClass(io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile.class,
                player.getBoundingBox().inflate(12), p -> p.getOwner() == player);
        h.assertTrue(shots.size() == 1 && ((NativeCastCarrier)shots.getFirst()).ironsNouveau$session().plan().spellLevel() == level,
                "Conversion evaluates crafted base plus amplify");
        h.assertTrue(mana.get() == 100000 - expected, "Final level determines real native fee");
        ((NativeCastCarrier)shots.getFirst()).ironsNouveau$session().finish(CastSession.EndReason.COMPLETED);
        h.assertTrue(SpellLevels.resolve(player, form.spellId(), Collections.nCopies(50, AugmentAmplify.INSTANCE)) == base + 50,
                "Mastery is a base, never a ceiling for amplifies");
        var heal = IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals("heal")).findFirst().orElseThrow();
        var nativeHeal = SpellRegistry.getSpell(heal.definition().spellId());
        var craftedHeal = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(nativeHeal, 3, craftedHeal); SpellProgress.crafted(player, craftedHeal);
        var target = h.spawn(net.minecraft.world.entity.EntityType.COW, 5, 2, 1);
        target.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH).setBaseValue(1000); target.setHealth(1);
        int balance = mana.get();
        heal.onResolve(new net.minecraft.world.phys.EntityHitResult(target), h.getLevel(), player,
                new SpellStats.Builder().setAugments(Collections.nCopies(2, AugmentAmplify.INSTANCE)).build(), resolver.spellContext, resolver);
        h.assertTrue(Math.abs(target.getHealth() - 1 - nativeHeal.getSpellPower(5, player)) < .01
                && balance - mana.get() == nativeHeal.getManaCost(5), "Effects share crafted base, amplification, native output and fee");
        player.discard(); h.succeed();
    }
}
