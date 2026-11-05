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
public final class NativeFormAugment extends AbstractAugment implements dev.arsconflux.api.projectile.ProjectileForm {
    private final ResourceLocation spellId;
    private final NativeCastAdapter adapter;
    private final int manaCost;
    public NativeFormAugment(String spell, String name, SpellSchool school,
                              NativeCastAdapter adapter) {
        this(spell, name, school, adapter, 10, SpellTier.ONE);
    }
    public NativeFormAugment(String spell, String name, SpellSchool school,
                              NativeCastAdapter adapter, int manaCost, SpellTier tier) {
        this(dev.ironsnouveau.platform.Locations.id("irons_nouveau", "glyph_" + spell),
                dev.ironsnouveau.platform.Locations.id("irons_spellbooks", spell), name, school, adapter, manaCost, tier);
    }
    public NativeFormAugment(ResourceLocation glyph, ResourceLocation spell, String name, SpellSchool school,
                             NativeCastAdapter adapter, int manaCost, SpellTier tier) {
        super(glyph, name);
        spellId = spell;
        this.adapter = adapter;
        this.manaCost = manaCost;
        spellSchools.add(school); school.addSpellPart(this);
    }
    @Override public boolean replace(com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell carrier,
                                     dev.arsconflux.api.projectile.CarrierProfile profile) {
        return dev.ironsnouveau.casting.NativeCasting.convert(carrier);
    }
    @Override public dev.arsconflux.api.resource.ResourceCharge initialCharge(
            com.hollingsworth.arsnouveau.api.spell.SpellContext context, java.util.List<AbstractAugment> augments) {
        return dev.ironsnouveau.casting.PresetTools.scoped(context, () -> {
            var caster = dev.arsconflux.api.context.CastContext.of(context, null, null).caster();
            if (caster == null) return null;
            int level = dev.ironsnouveau.casting.SpellLevels.resolve(caster, spellId, augments);
            return dev.ironsnouveau.casting.TriggerMana.of(context, caster).charge(spellId, level);
        });
    }
    public ResourceLocation spellId() { return spellId; }
    @Override public String getLocalizationKey() {
        return spellId!=null&&!spellId.getNamespace().equals("irons_spellbooks")
                ? "spell."+spellId.getNamespace()+"."+spellId.getPath():super.getLocalizationKey();
    }
    @Override public net.minecraft.network.chat.Component getBookDescLang() {
        return spellId!=null&&dev.ironsnouveau.compat.wind.WindManualCatalog.owns(spellId)
                ? net.minecraft.network.chat.Component.translatable("irons_nouveau.wind."+spellId.getPath()):super.getBookDescLang();
    }
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
