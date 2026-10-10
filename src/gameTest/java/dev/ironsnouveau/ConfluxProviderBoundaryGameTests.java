package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAmplify;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import dev.arsconflux.api.glyph.SpellValidators;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.*;
import net.minecraft.gametest.framework.*;
import net.neoforged.neoforge.gametest.*;
import java.util.List;

@GameTestHolder("irons_nouveau_conflux_api")
@PrefixGameTestTemplate(false)
public final class ConfluxProviderBoundaryGameTests {
    @GameTest(template = "empty")
    public static void foreignPayloadCanCombineWithIronEffectWithoutPairwiseIntegration(GameTestHelper helper) {
        var foreign = new ForeignPayload();
        var heal = IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals("heal")).findFirst().orElseThrow();
        helper.assertTrue(SpellValidators.validate(List.of(MethodProjectile.INSTANCE, foreign, AugmentAmplify.INSTANCE, heal)).isEmpty(),
                "Iron must not require its own payload on a phrase owned by another Core provider");
        helper.assertTrue(!SpellValidators.validate(List.of(MethodProjectile.INSTANCE, foreign, IronsNouveau.forms().get(0))).isEmpty(),
                "Shared validation still rejects two providers replacing the same projectile");
        helper.succeed();
    }
    private static final class ForeignPayload extends AbstractAugment implements ProjectileForm {
        ForeignPayload() { super(CarrierRegistry.id("test_provider:glyph_payload"), "Foreign Payload"); }
        public int getDefaultManaCost() { return 0; }
        public ResourceCharge initialCharge(SpellContext context, List<AbstractAugment> augments) {
            return new ResourceCharge(new ArsManaAccount(context.getCaster()), 5);
        }
        public boolean replace(EntityProjectileSpell carrier, CarrierProfile profile) { return false; }
    }
}
