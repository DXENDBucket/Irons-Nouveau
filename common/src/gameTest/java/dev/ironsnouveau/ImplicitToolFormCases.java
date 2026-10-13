package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.ArsNouveauAPI;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import dev.arsconflux.api.glyph.ArsSpellAccess;
import dev.ironsnouveau.casting.NativeCasting;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import java.util.*;
import java.util.function.Function;

/** Actual native tool inscription transforms, not a second implementation of their defaults. */
public final class ImplicitToolFormCases {
    public static void inscriptionAndCasting(GameTestHelper h,
            Function<Spell, Spell> wand, Function<Spell, Spell> sword, LivingEntity caster) {
        var editor = ArsNouveauAPI.getInstance().getSpellCraftingSpellValidator();
        var casting = ArsNouveauAPI.getInstance().getSpellCastingSpellValidator();
        for (var form : GlyphCatalog.forms()) {
            var template = new Spell(form);
            h.assertTrue(editor.validate(new ArrayList<>(ArsSpellAccess.parts(template))).isEmpty(), "Leading payload template: " + form.spellId());
            h.assertTrue(!casting.validate(new ArrayList<>(ArsSpellAccess.parts(template))).isEmpty(), "Template alone cannot cast");
            var inscribed = wand.apply(template);
            var parts = ArsSpellAccess.parts(inscribed);
            h.assertTrue(parts.get(0) == MethodProjectile.INSTANCE && parts.get(1) == AugmentAccelerate.INSTANCE,
                    "Wand retains its real native method and acceleration");
            h.assertTrue(editor.validate(new ArrayList<>(parts)).isEmpty(), "Inscribed wand accepts payload: " + form.spellId());
            h.assertTrue(casting.validate(new ArrayList<>(parts)).isEmpty(), "Inscribed wand is castable");
            h.assertTrue(!NativeCasting.validate(ArsSpellAccess.parts(sword.apply(template))).isEmpty(), "Touch sword remains incompatible");
            h.assertTrue(ArsSpellAccess.parts(template).equals(List.of(form)), "Inscription does not mutate source template");
        }
        var firebolt = GlyphCatalog.forms().stream().filter(f -> f.spellId().getPath().equals("firebolt")).findFirst().orElseThrow();
        var icicle = GlyphCatalog.forms().stream().filter(f -> f.spellId().getPath().equals("icicle")).findFirst().orElseThrow();
        h.assertTrue(!editor.validate(new ArrayList<>(List.of(firebolt, icicle))).isEmpty(), "Template still rejects duplicate payloads");
        var unsupported = new Spell(firebolt, AugmentPierce.INSTANCE);
        h.assertTrue(editor.validate(new ArrayList<>(ArsSpellAccess.parts(unsupported))).isEmpty(), "Unknown method restrictions defer until inscription");
        h.assertTrue(!editor.validate(new ArrayList<>(ArsSpellAccess.parts(wand.apply(unsupported)))).isEmpty(), "Known wand method rejects unsupported pierce");
        var powered = wand.apply(new Spell(firebolt, AugmentAmplify.INSTANCE));
        h.assertTrue(editor.validate(new ArrayList<>(ArsSpellAccess.parts(powered))).isEmpty(), "Payload level augment works after inscription");
        var source = new LivingCaster(caster) {
            public boolean enoughMana(int cost) { return true; }
            public void expendMana(int cost) {}
        };
        var resolver = new SpellResolver(new SpellContext(h.getLevel(), powered, caster, source, ItemStack.EMPTY));
        h.assertTrue(resolver.onCast(ItemStack.EMPTY, h.getLevel()), "Completed wand spell actually casts");
        h.assertTrue(!h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class, caster.getBoundingBox().inflate(16),
                p -> p.getOwner() == caster).isEmpty(), "Native Iron projectile is spawned");
        h.succeed();
    }
    private ImplicitToolFormCases() {}
}
