package dev.ironsnouveau;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.ironsnouveau.casting.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.gametest.*;
@GameTestHolder("irons_nouveau")
@PrefixGameTestTemplate(false)
public final class ForgeSmokeTests {
    static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var world = h.getLevel(); var server = world.getServer();
        io.redspace.ironsspellbooks.api.config.SpellConfigManager.onDatapackSync(new net.minecraftforge.event.OnDatapackSyncEvent(server.getPlayerList(), null));
        var player = new net.minecraft.server.level.ServerPlayer(server, world,
            new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "ForgeStudent")) {
                @Override public void displayClientMessage(net.minecraft.network.chat.Component message, boolean actionBar) { System.out.println("TEST MESSAGE: " + message.getString()); }
            };
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        player.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(server, connection, player) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {}
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet, net.minecraft.network.PacketSendListener listener) {}
        };
        player.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1, 3, 1)));
        player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
        CapabilityRegistry.getMana(player).ifPresent(m -> { m.setMaxMana(1000); m.setMana(1000); });
        player.getAttribute(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get()).setBaseValue(10000);
        io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(player).setMana(1000);
        return player;
    }
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void registrationHealingAndMana(GameTestHelper h) {
        h.assertTrue(IronsNouveau.glyphs().size() == 84 && IronsNouveau.forms().size() == 19, "All 103 glyphs register");
        var cow = player(h); cow.setHealth(2);
        var mana = CapabilityRegistry.getMana(cow).orElseThrow(IllegalStateException::new); mana.setMana(1000);
        var heal = IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals("heal")).findFirst().orElseThrow();
        dev.ironsnouveau.progression.SpellProgress.accept(cow, dev.ironsnouveau.progression.CraftedSpells.EMPTY.record(heal.definition().spellId(), 1));
        var resolver = new SpellResolver(SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE, heal), cow, ItemStack.EMPTY));
        h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "Self cast accepted");
        h.assertTrue(cow.getHealth() > 2, "Native healing executed");
        var iron = io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(cow);
        h.assertTrue(iron.getMana() < 1000, "Real trigger spends Iron mana");
        iron.setMana(0); cow.setHealth(2);
        heal.onResolve(new net.minecraft.world.phys.EntityHitResult(cow), h.getLevel(), cow,
            new SpellStats.Builder().build(), resolver.spellContext, resolver);
        h.assertTrue(cow.getHealth() == 2, "Empty mana blocks effect");
        var data = dev.ironsnouveau.progression.CraftedSpells.EMPTY.record(heal.definition().spellId(), 4);
        h.assertTrue(dev.ironsnouveau.progression.CraftedSpells.load(data.save()).level(heal.definition().spellId()) == 4, "Progress NBT roundtrip");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void nativeProjectileConversion(GameTestHelper h) {
        var cow = player(h);
        CapabilityRegistry.getMana(cow).ifPresent(m -> m.setMana(1000));
        var form = IronsNouveau.forms().stream().filter(g -> g.spellId().getPath().equals("firebolt")).findFirst().orElseThrow();
        dev.ironsnouveau.progression.SpellProgress.accept(cow, dev.ironsnouveau.progression.CraftedSpells.EMPTY.record(form.spellId(), 1));
        var resolver = new SpellResolver(SpellContext.fromEntity(new Spell(MethodProjectile.INSTANCE, form), cow, ItemStack.EMPTY));
        h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "Projectile cast accepted");
        var shots = h.getLevel().getEntitiesOfClass(io.redspace.ironsspellbooks.entity.spells.firebolt.FireboltProjectile.class, cow.getBoundingBox().inflate(8));
        h.assertTrue(shots.size() == 1, "Ars carrier becomes a native projectile");
        h.assertTrue(((NativeCastCarrier)shots.get(0)).ironsNouveau$session() != null, "Projectile has managed session");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 80)
    public static void scrollCraftUnlockAndClone(GameTestHelper h) {
        var player = player(h);
        var nativeSpell = SpellRegistry.FIREBOLT_SPELL.get(); var id = nativeSpell.getSpellResource();
        var form = IronsNouveau.forms().stream().filter(g -> g.spellId().equals(id)).findFirst().orElseThrow();
        var known = CapabilityRegistry.getPlayerDataCap(player).orElseThrow(IllegalStateException::new);
        h.assertTrue(!known.unlockGlyph(form), "Glyph cannot bypass personal crafting");
        var pos = new net.minecraft.core.BlockPos(3, 1, 3);
        h.setBlock(pos, io.redspace.ironsspellbooks.registries.BlockRegistry.SCROLL_FORGE_BLOCK.get());
        var menu = new io.redspace.ironsspellbooks.gui.scroll_forge.ScrollForgeMenu(1, player.getInventory(), h.getBlockEntity(pos));
        player.containerMenu = menu;
        menu.getBlankScrollSlot().set(new ItemStack(net.minecraft.world.item.Items.PAPER));
        menu.getFocusSlot().set(new ItemStack(net.minecraft.world.item.Items.BLAZE_ROD));
        menu.getInkSlot().set(new ItemStack(io.redspace.ironsspellbooks.item.InkItem.getInkForRarity(io.redspace.ironsspellbooks.api.spells.SpellRarity.COMMON)));
        menu.setRecipeSpell(nativeSpell);
        h.assertTrue(!menu.getResultSlot().getItem().isEmpty(), "Forge has a valid recipe");
        menu.clicked(39, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        h.assertTrue(dev.ironsnouveau.progression.SpellProgress.craftedLevel(player, id) == 1 && known.knowsGlyph(form), "Committed craft records level and unlocks glyph");
        var clone = player(h);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(new net.minecraftforge.event.entity.player.PlayerEvent.Clone(clone, player, true));
        h.assertTrue(dev.ironsnouveau.progression.SpellProgress.craftedLevel(clone, id) == 1, "Death clone retains progress");
        h.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 160)
    public static void activeChantAndCooldown(GameTestHelper h) {
        var player = player(h); player.setHealth(2);
        var heal = IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals("greater_heal")).findFirst().orElseThrow();
        dev.ironsnouveau.progression.SpellProgress.accept(player, dev.ironsnouveau.progression.CraftedSpells.EMPTY.record(heal.definition().spellId(), 1));
        var hand = net.minecraft.world.InteractionHand.MAIN_HAND;
        var stack = new ItemStack(net.minecraft.world.item.Items.PAPER);
        player.setItemInHand(hand, stack);
        var caster = new SpellCaster(stack);
        var spell = new Spell(MethodSelf.INSTANCE, heal);
        int ticks = ChantTiming.ticks(spell, player, dev.ironsnouveau.config.SpellLevelConfig.chantMode());
        h.assertTrue(ticks > 0, "Native healing has a chant duration");
        caster.castSpell(h.getLevel(), player, hand, null, spell);
        h.assertTrue(ActiveChanting.isChanting(player) && player.getHealth() == 2, "Old Ars active entry defers until chant ends");
        h.runAfterDelay(ticks + 2, () -> {
            h.assertTrue(!ActiveChanting.isChanting(player) && player.getHealth() > 2, "Chant releases the actual effect");
            h.assertTrue(!ActiveCooldowns.allowed(spell, player), "Release starts shared native cooldown");
            h.succeed();
        });
    }
}
