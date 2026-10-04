package dev.ironsnouveau.glyph;

import com.hollingsworth.arsnouveau.api.spell.AbstractAugment;
import com.hollingsworth.arsnouveau.api.spell.SpellSchool;
import com.hollingsworth.arsnouveau.api.spell.SpellTier;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import dev.ironsnouveau.api.GlyphAccessEvent;
import dev.ironsnouveau.api.NativeCastAdapter;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Selects native execution; applies no damage, spawning or mana changes while Ars gathers augments. */
public final class NativeFormAugment extends AbstractAugment {
    private final ResourceLocation spellId;
    private final NativeCastAdapter adapter;
    private final int manaCost;
    public NativeFormAugment(String spell, String name, SpellSchool school,
                              NativeCastAdapter adapter) {
        this(spell, name, school, adapter, 10, SpellTier.ONE);
    }
    public NativeFormAugment(String spell, String name, SpellSchool school,
                              NativeCastAdapter adapter, int manaCost, SpellTier tier) {
        super(ResourceLocation.fromNamespaceAndPath("irons_nouveau", "glyph_" + spell), name);
        spellId = ResourceLocation.fromNamespaceAndPath("irons_spellbooks", spell);
        this.adapter = adapter;
        this.manaCost = manaCost;
        spellSchools.add(school); school.addSpellPart(this);
    }
    public ResourceLocation spellId() { return spellId; }
    public NativeCastAdapter adapter() { return adapter; }
    @Override public int getDefaultManaCost() { return 0; }
    @Override public int getCastingCost() { return 0; }
    @Override public SpellTier defaultTier() { return SpellTier.ONE; }
    @Override public SpellTier getConfigTier() { return SpellTier.ONE; }
    @Override public boolean shouldShowInUnlock() { return !dev.ironsnouveau.config.SpellLevelConfig.requiresCrafting(); }
    @Override public Glyph getGlyph() {
        if (glyphItem == null) glyphItem = new Glyph(this) {
            @Override public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
                if (!world.isClientSide && (!SpellRegistry.getSpell(spellId).isEnabled()
                        || !GlyphAccessEvent.allowed(player, getRegistryName(), spellId, 1, GlyphAccessEvent.Action.LEARN)))
                    return InteractionResultHolder.fail(player.getItemInHand(hand));
                return super.use(world, player, hand);
            }
        };
        return glyphItem;
    }
}
