package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import dev.ironsnouveau.api.PresetSpellTool;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.function.Supplier;

/** Authorization is scoped to the actual Ars caster tool, never merely to an item in the inventory. */
public final class PresetTools {
    public record Token(LivingEntity caster, ItemStack stack) {}
    private static final ThreadLocal<Token> CURRENT = new ThreadLocal<>();
    private PresetTools() {}
    public static Token current() { return CURRENT.get(); }
    public static int level(LivingEntity caster, ResourceLocation spell) {
        var token = current();
        if (token == null || token.caster != caster) return 0;
        if (BoundSpellWeapons.hasBinding(token.stack)) return BoundSpellWeapons.presetLevel(token.stack, spell);
        return token.stack.getItem() instanceof PresetSpellTool tool ? Math.max(0, tool.presetLevel(token.stack, spell)) : 0;
    }
    public static <T> T scoped(Token token, Supplier<T> action) {
        var previous = current();
        if (token == null) CURRENT.remove(); else CURRENT.set(token);
        try { return action.get(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
    public static <T> T scoped(SpellContext context, Supplier<T> action) {
        var tool = context.getCasterTool();
        var caster = dev.arsconflux.api.glyph.ArsSpellAccess.caster(context);
        return scoped(caster != null && isPreset(tool) ? new Token(caster, tool.copy()) : null, action);
    }
    public static boolean isPreset(ItemStack tool) {
        return BoundSpellWeapons.hasBinding(tool) || tool.getItem() instanceof PresetSpellTool;
    }
}
