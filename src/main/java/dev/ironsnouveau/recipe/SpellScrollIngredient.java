package dev.ironsnouveau.recipe;

import com.mojang.serialization.MapCodec;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.item.Scroll;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.crafting.ICustomIngredient;
import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import java.util.function.Supplier;
import java.util.stream.Stream;

/** Matches the native spell, independently of rarity, ownership, and who made the scroll. */
public record SpellScrollIngredient(ResourceLocation spell) implements ICustomIngredient {
    public static final MapCodec<SpellScrollIngredient> CODEC = ResourceLocation.CODEC.fieldOf("spell")
            .xmap(SpellScrollIngredient::new, SpellScrollIngredient::spell);
    private static final DeferredRegister<IngredientType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.INGREDIENT_TYPES, "irons_nouveau");
    private static final Supplier<IngredientType<SpellScrollIngredient>> TYPE =
            TYPES.register("spell_scroll", () -> new IngredientType<>(CODEC));
    public static void register(IEventBus bus) { TYPES.register(bus); }

    @Override public boolean test(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof Scroll)) return false;
        var container = ISpellContainer.get(stack);
        if (container == null) return false;
        var data = container.getSpellAtIndex(0);
        return data != null && data.getSpell() != SpellRegistry.none() && data.getLevel() > 0
                && data.getSpell().getSpellResource().equals(spell);
    }
    @Override public Stream<ItemStack> getItems() {
        var nativeSpell = SpellRegistry.getSpell(spell);
        if (nativeSpell == SpellRegistry.none()) return Stream.empty();
        var example = new ItemStack(ItemRegistry.SCROLL.get());
        ISpellContainer.createScrollContainer(nativeSpell, nativeSpell.getMinLevel(), example);
        return Stream.of(example);
    }
    @Override public boolean isSimple() { return false; }
    @Override public IngredientType<?> getType() { return TYPE.get(); }
}
