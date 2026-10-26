package dev.ironsnouveau.recipe;

import io.redspace.ironsspellbooks.api.spells.AbstractSpell;
import io.redspace.ironsspellbooks.api.spells.ISpellContainer;
import io.redspace.ironsspellbooks.registries.ItemRegistry;
import net.minecraft.world.item.ItemStack;
import java.util.stream.*;

/** JEI indexes these concrete stacks; the ingredient predicate still accepts any positive level. */
public final class ScrollVariants {
    private ScrollVariants() {}
    public static Stream<ItemStack> of(AbstractSpell spell) {
        return IntStream.rangeClosed(Math.max(1,spell.getMinLevel()),spell.getMaxLevel()).mapToObj(level->{
            var stack=new ItemStack(ItemRegistry.SCROLL.get());
            ISpellContainer.createScrollContainer(spell,level,stack);return stack;
        });
    }
}
