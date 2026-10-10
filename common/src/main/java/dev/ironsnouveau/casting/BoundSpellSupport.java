package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.*;
import dev.ironsnouveau.glyph.BridgeGlyph;
import dev.ironsnouveau.casting.BoundSpellWeapons.Binding;
import dev.ironsnouveau.glyph.NativeFormAugment;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.CastSource;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import java.util.*;

/** A sealed, reusable Ars recipe on any item. This is deliberately not an editable Ars caster. */
public abstract class BoundSpellSupport {
    protected BoundSpellSupport() {}
    public static void appendTooltip(ItemStack stack, List<Component> lines) {
        var spell = program(stack);
        if (spell == null) return;
        var spells = ironSpells(spell);
        String name = dev.ironsnouveau.platform.BoundSpellData.name(spell);
        if (!name.isBlank()) lines.add(Component.translatable("irons_nouveau.bound_weapon.contains", name)
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        else if (spells.isEmpty()) lines.add(Component.translatable("irons_nouveau.bound_weapon.unnamed")
                .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        for (var id : spells) {
            var nativeSpell = SpellRegistry.getSpell(id);
            if (nativeSpell != SpellRegistry.none()) lines.add(Component.translatable("irons_nouveau.bound_weapon.contains",
                    new io.redspace.ironsspellbooks.api.spells.SpellData(nativeSpell, presetLevel(stack, id)).getDisplayName())
                    .withStyle(net.minecraft.ChatFormatting.LIGHT_PURPLE));
        }
    }
    public static boolean hasBinding(ItemStack stack) { return dev.ironsnouveau.platform.BoundSpellData.has(stack); }
    public static void clearBinding(ItemStack stack) { dev.ironsnouveau.platform.BoundSpellData.clear(stack); }
    public static Spell program(ItemStack stack) {
        var data = dev.ironsnouveau.platform.BoundSpellData.get(stack);
        return data == null ? null : data.spell();
    }
    public static ResourceLocation ironSpell(AbstractSpellPart part) {
        return part instanceof BridgeGlyph g ? g.definition().spellId()
                : part instanceof NativeFormAugment g ? g.spellId() : null;
    }
    public static Set<ResourceLocation> ironSpells(Spell spell) {
        var result = new LinkedHashSet<ResourceLocation>();
        for (var part : dev.arsconflux.api.glyph.ArsSpellAccess.parts(spell)) { var id = ironSpell(part); if (id != null) result.add(id); }
        return result;
    }
    /** Only spells actually present in this recipe can be authorized by its fixed-level map. */
    public static int presetLevel(ItemStack stack, ResourceLocation id) {
        var data = dev.ironsnouveau.platform.BoundSpellData.get(stack);
        return data != null && ironSpells(data.spell()).contains(id) ? data.ironLevels().getOrDefault(id, 1) : 0;
    }
    public static void bind(ItemStack stack, Spell spell, Map<ResourceLocation, Integer> levels) {
        if (stack.isEmpty() || spell.isEmpty() || spell.getCastMethod() == null)
            throw new IllegalArgumentException("A bound weapon needs a nonempty Ars recipe with a cast method");
        var actual = new HashMap<ResourceLocation, Integer>();
        for (var id : ironSpells(spell)) {
            int level = levels.getOrDefault(id, 1);
            if (level < 1) throw new IllegalArgumentException("Spell levels must be positive");
            actual.put(id, level);
        }
        // Snapshot the recipe rather than keeping the mutable list supplied by a caller.
        dev.ironsnouveau.platform.BoundSpellData.set(stack,
                new Binding(dev.ironsnouveau.platform.BoundSpellData.copy(spell), actual));
    }
    public static void bind(ItemStack stack, Spell spell, int level) {
        var levels = new HashMap<ResourceLocation, Integer>();
        for (var id : ironSpells(spell)) levels.put(id, level);
        bind(stack, spell, levels);
    }
    public static boolean busy(LivingEntity caster) {
        return ActiveChanting.isChanting(caster) || CastSessions.isCasting(caster);
    }
    private static boolean localCooldowns(LivingEntity caster) {
        return !(caster instanceof ServerPlayer) || dev.ironsnouveau.platform.Casters.isFake(caster);
    }
    private static boolean creativeFreeCooldown(LivingEntity caster) {
        return caster instanceof Player p && p.isCreative()
                && !io.redspace.ironsspellbooks.config.ServerConfigs.CREATIVE_COOLDOWN.get();
    }
    private static String cooldownKey(ResourceLocation id) { return "irons_nouveau_bound_cd_" + id; }
    public static boolean available(LivingEntity caster, ItemStack stack) {
        var spell = program(stack);
        if (spell == null || !caster.isAlive() || busy(caster)) return false;
        for (var id : ironSpells(spell)) {
            var nativeSpell = SpellRegistry.getSpell(id);
            if (nativeSpell == SpellRegistry.none() || !nativeSpell.isEnabled()) return false;
            if (localCooldowns(caster) && !creativeFreeCooldown(caster)
                    && caster.getPersistentData().getLong(cooldownKey(id)) > caster.level().getGameTime()) return false;
        }
        return PresetTools.scoped(new PresetTools.Token(caster, stack.copy()), () -> {
            var context = SpellContext.fromEntity(spell, caster, stack);
            var resolver = new SpellResolver(context).withSilent(true);
            if (!resolver.canCast(caster)) return false;
            // Do not require the whole composition's cost: later triggers may legitimately fail.
            // This only avoids starting empty-mana channels or launching an unusable first form.
            int action = 0;
            var parts = dev.arsconflux.api.glyph.ArsSpellAccess.parts(spell);
            for (int i = 0; i < parts.size(); i++) {
                var part = parts.get(i);
                if (!(part instanceof AbstractAugment)) action = i;
                var id = ironSpell(part);
                if (id != null) return TriggerMana.of(context, caster).canBegin(id,
                        SpellLevels.resolve(caster, id, spell.getAugments(action, caster)), resolver.getResolveCost());
            }
            return true;
        });
    }
    public static boolean cast(LivingEntity caster, ItemStack stack, InteractionHand hand) {
        if (!caster.level().isClientSide && caster.getItemInHand(hand) == stack && hasBinding(stack)
                && dev.arsconflux.api.interaction.InputSessions.route(caster, hand, program(stack))) return true;
        if (caster.level().isClientSide || caster.getItemInHand(hand) != stack || !available(caster, stack)) return false;
        var spell = program(stack);
        var snapshot = stack.copy();
        return PresetTools.scoped(new PresetTools.Token(caster, snapshot), () -> {
            if (!ActiveCooldowns.allowed(spell, caster)) return false;
            if (ActiveChanting.defer(spell, caster, hand, () -> {
                if (caster.getItemInHand(hand) == stack && ItemStack.matches(stack, snapshot)) release(caster, stack);
            })) return true;
            return release(caster, stack);
        });
    }
    private static boolean release(LivingEntity caster, ItemStack stack) {
        if (!available(caster, stack)) return false;
        var spell = program(stack);
        return PresetTools.scoped(new PresetTools.Token(caster, stack.copy()), () -> {
            var resolver = new SpellResolver(SpellContext.fromEntity(spell, caster, stack)).withSilent(true);
            boolean success = ActiveCooldowns.execute(spell, caster, CastSource.SWORD,
                    () -> resolver.onCast(stack, caster.level()), false);
            if (success && localCooldowns(caster) && !creativeFreeCooldown(caster))
                for (var id : ironSpells(spell)) caster.getPersistentData().putLong(cooldownKey(id),
                        caster.level().getGameTime() + SpellRegistry.getSpell(id).getSpellCooldown());
            return success;
        });
    }
    public static void registerCommands(com.mojang.brigadier.CommandDispatcher<net.minecraft.commands.CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("ironsnouveau").requires(s -> s.hasPermission(2))
                .then(Commands.literal("bind_weapon")
                        .then(Commands.argument("level", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1))
                                .executes(c -> {
                                    var player = c.getSource().getPlayerOrException();
                                    var source = player.getOffhandItem();
                                    var target = player.getMainHandItem();
                                    var spell = dev.ironsnouveau.platform.BoundSpellData.fromBook(source);
                                    if (hasBinding(source) || spell == null || target.isEmpty()) {
                                        c.getSource().sendFailure(Component.translatable("irons_nouveau.bound_weapon.invalid_source"));
                                        return 0;
                                    }
                                    if (spell.isEmpty() || spell.getCastMethod() == null) return 0;
                                    bind(target, spell, com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "level"));
                                    c.getSource().sendSuccess(() -> Component.translatable("irons_nouveau.bound_weapon.bound"), false);
                                    return 1;
                                })))
                .then(Commands.literal("clear_weapon_binding").executes(c -> {
                    clearBinding(c.getSource().getPlayerOrException().getMainHandItem());
                    return 1;
                })));
    }
}
