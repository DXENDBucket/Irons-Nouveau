package dev.ironsnouveau.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** A finished tool may authorize its own fixed payload without teaching personal glyph mastery. */
public interface PresetSpellTool {
    /** Zero means this tool does not contain this spell. */
    int presetLevel(ItemStack stack, ResourceLocation spell);
}
