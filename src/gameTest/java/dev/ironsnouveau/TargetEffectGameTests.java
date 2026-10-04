package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.augment.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.ironsnouveau.glyph.BridgeGlyph;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(IronsNouveau.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TargetEffectGameTests {
    private static BridgeGlyph effect(String id) {
        return IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow();
    }
    private static LivingEntity cow(GameTestHelper h, int x, int z) {
        var cow = h.spawn(EntityType.COW, x, 2, z); cow.setNoAi(true); cow.setNoGravity(true); return cow;
    }
    private static SpellResolver resolver(LivingEntity caster, Spell spell) {
        var source = new LivingCaster(caster) {
            @Override public boolean enoughMana(int amount) { return true; }
            @Override public void expendMana(int amount) {}
        };
        return new SpellResolver(new SpellContext(caster.level(), spell, caster, source, ItemStack.EMPTY));
    }
    @GameTest(template = "empty", batch = "targeted_effects")
    public static void targetedStatusesShareExistingEffectPipeline(GameTestHelper h) {
        var caster = cow(h, 1, 1); var target = cow(h, 3, 1); var nearby = cow(h, 3, 2);
        h.assertTrue(resolver(caster, new Spell(MethodSelf.INSTANCE, effect("haste"))).onCast(ItemStack.EMPTY, h.getLevel()), "Self uses normal Ars effect pipeline");
        h.assertTrue(caster.hasEffect(MobEffectRegistry.HASTENED), "Haste affects the self target");
        h.assertTrue(resolver(caster, new Spell(MethodTouch.INSTANCE, effect("slow"), AugmentAmplify.INSTANCE,
                AugmentExtendTime.INSTANCE, AugmentAOE.INSTANCE)).onCastOnEntity(ItemStack.EMPTY, target, InteractionHand.MAIN_HAND), "Touch accepts status effect");
        var slow = target.getEffect(MobEffectRegistry.SLOWED);
        var nativeSlow = (io.redspace.ironsspellbooks.spells.evocation.SlowSpell)io.redspace.ironsspellbooks.api.registry.SpellRegistry.getSpell(effect("slow").definition().spellId());
        h.assertTrue(slow != null && slow.getAmplifier() == nativeSlow.getAmplifier(2, caster)
                && slow.getDuration() == (int)(nativeSlow.getDuration(2, caster) * 1.5), "Level two native status with independent Ars duration");
        h.assertTrue(nearby.hasEffect(MobEffectRegistry.SLOWED), "Shared area targeting includes nearby creature");
        h.assertTrue(!caster.hasEffect(MobEffectRegistry.SLOWED), "Harmful area effect excludes caster");
        h.assertTrue(resolver(caster, new Spell(MethodTouch.INSTANCE, effect("blight"), AugmentAmplify.INSTANCE,
                AugmentAmplify.INSTANCE, AugmentAmplify.INSTANCE, AugmentAmplify.INSTANCE))
                .onCastOnEntity(ItemStack.EMPTY, target, InteractionHand.MAIN_HAND), "Blight uses normal touch resolution");
        var blight = target.getEffect(MobEffectRegistry.BLIGHT);
        h.assertTrue(blight != null && blight.getAmplifier() == 4, "Four Amplify glyphs produce native level five Blight");
        h.succeed();
    }
}
