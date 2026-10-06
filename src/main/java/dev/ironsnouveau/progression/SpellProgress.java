package dev.ironsnouveau.progression;

import com.hollingsworth.arsnouveau.api.spell.AbstractSpellPart;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.ironsnouveau.IronsNouveau;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.glyph.NativeFormAugment;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.registries.DeferredRegister;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Only successful workstation crafts award history; owning, receiving and casting items never do. */
public final class SpellProgress {
    private static final String KEY = "irons_nouveau:crafted_spells";
    private SpellProgress() {}
    public static CraftedSpells read(Player player) { return CraftedSpells.load(player.getPersistentData().getCompound(KEY)); }
    public static void accept(Player player, CraftedSpells data) { player.getPersistentData().put(KEY, data.save()); }
    private static void sync(ServerPlayer player) { dev.ironsnouveau.network.ForgeNetwork.sendToPlayer(player, new dev.ironsnouveau.network.ProgressPayload(read(player))); }
    public static void register(IEventBus modBus, IEventBus gameBus) {
        gameBus.addListener(SpellProgress::login);
        gameBus.addListener(SpellProgress::tooltip);
        gameBus.addListener(SpellProgress::clonePlayer);
        gameBus.addListener(SpellProgress::respawn);
        gameBus.addListener(SpellProgress::changedDimension);
    }
    private static void clonePlayer(PlayerEvent.Clone event) { accept(event.getEntity(), read(event.getOriginal())); }
    private static void respawn(PlayerEvent.PlayerRespawnEvent event) { if (event.getEntity() instanceof ServerPlayer p) sync(p); }
    private static void changedDimension(PlayerEvent.PlayerChangedDimensionEvent event) { if (event.getEntity() instanceof ServerPlayer p) sync(p); }
    public static int craftedLevel(Player player, ResourceLocation spell) { return read(player).level(spell); }
    public static int baseLevel(LivingEntity caster, ResourceLocation spell) {
        int configured = SpellLevelConfig.mode() == SpellLevelConfig.Mode.FIXED ? SpellLevelConfig.configuredLevel(spell) : 1;
        if (!(caster instanceof Player player)) {
            var event = new dev.ironsnouveau.api.NativeSpellLevelEvent(caster, spell, configured);
            net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event); return event.level();
        }
        // External mob integrations retain their level event; players use this synchronized server policy.
        if (player.isCreative() && SpellLevelConfig.creativeNativeMax())
            return Math.max(1, SpellRegistry.getSpell(spell).getMaxLevel());
        return SpellLevelConfig.mode() == SpellLevelConfig.Mode.FIXED ? configured : Math.max(1, craftedLevel(player, spell));
    }
    public static boolean canUse(LivingEntity caster, ResourceLocation spell) {
        return !SpellLevelConfig.requiresCrafting() || !(caster instanceof Player player) || player.isCreative() || craftedLevel(player, spell) > 0;
    }
    public static ResourceLocation spellId(AbstractSpellPart part) {
        if (part instanceof BridgeGlyph glyph) return glyph.definition().spellId();
        if (part instanceof NativeFormAugment form) return form.spellId();
        return null;
    }
    private static Stream<AbstractSpellPart> glyphs() { return Stream.concat(IronsNouveau.glyphs().stream(), IronsNouveau.forms().stream()); }
    /** Creative uses Ars's saved knowledge; survival additionally requires personal scroll crafting. */
    public static java.util.Collection<AbstractSpellPart> visibleKnowledge(Player player, java.util.Collection<AbstractSpellPart> saved) {
        if (player.isCreative()) return saved;
        var visible = new java.util.LinkedHashSet<AbstractSpellPart>();
        for (var part : saved) {
            var spell = spellId(part);
            if (spell == null || canUse(player, spell)) visible.add(part);
        }
        return visible;
    }
    public static void crafted(Player actor, ItemStack output) {
        if (!(actor instanceof ServerPlayer player) || player.isCreative()
                || player instanceof net.minecraftforge.common.util.FakePlayer || !(output.getItem() instanceof Scroll)) return;
        var container = ISpellContainer.get(output);
        if (container == null) return;
        var data = container.getSpellAtIndex(0);
        if (data == null || data.getSpell() == SpellRegistry.none() || data.getLevel() < 1) return;
        var spell = data.getSpell().getSpellResource();
        var before = read(player); var after = before.record(spell, data.getLevel());
        if (before != after) { accept(player, after); sync(player); }
        if (SpellLevelConfig.requiresCrafting()) unlock(player, spell);
        if (before != after && glyphs().anyMatch(g -> spell.equals(spellId(g))))
            player.displayClientMessage(Component.translatable("irons_nouveau.study.progress", data.getSpell().getDisplayName(player), after.level(spell)), false);
    }
    private static void unlock(ServerPlayer player, ResourceLocation spell) {
        var cap = CapabilityRegistry.getPlayerDataCap(player).orElse(null);
        if (cap == null) return;
        boolean changed = false;
        for (var glyph : glyphs().filter(g -> spell.equals(spellId(g))).toList()) changed |= cap.unlockGlyph(glyph);
        if (changed) {
            CapabilityRegistry.EventHandler.syncPlayerCap(player);
            var mana = CapabilityRegistry.getMana(player).orElse(null);
            if (mana != null && mana.getGlyphBonus() < cap.getKnownGlyphs().size()) {
                mana.setGlyphBonus(cap.getKnownGlyphs().size()); com.hollingsworth.arsnouveau.common.network.Networking.sendToPlayerClient(new com.hollingsworth.arsnouveau.common.network.PacketUpdateMana(mana.getCurrentMana(), mana.getMaxMana(), mana.getGlyphBonus(), mana.getBookTier()), player);
            }
        }
    }
    private static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) sync(p);
        if (SpellLevelConfig.requiresCrafting() && event.getEntity() instanceof ServerPlayer player && !player.isCreative())
            read(player).levels().keySet().forEach(spell -> unlock(player, spell));
    }
    private static void tooltip(ItemTooltipEvent event) {
        if (event.getEntity() == null || !(event.getItemStack().getItem() instanceof Glyph glyph)) return;
        var spell = spellId(glyph.spellPart); if (spell == null) return;
        event.getToolTip().add(canUse(event.getEntity(), spell)
                ? Component.translatable("irons_nouveau.study.level", baseLevel(event.getEntity(), spell)).withStyle(ChatFormatting.BLUE)
                : Component.translatable("irons_nouveau.study.unfamiliar").withStyle(ChatFormatting.GRAY));
    }
}
