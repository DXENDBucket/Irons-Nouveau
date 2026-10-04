package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.IMagicSummon;
import io.redspace.ironsspellbooks.entity.spells.summoned_weapons.SummonedWeaponEntity;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;
import net.neoforged.neoforge.gametest.*;
import java.util.*;

@GameTestHolder(IronsNouveau.MOD_ID)
@PrefixGameTestTemplate(false)
public final class SummonLifetimeGameTests {
    @GameTest(template = "empty", batch = "summon_lifetime", timeoutTicks = 35)
    public static void expiredWeaponsLeaveWorldAndOwnerRegistry(GameTestHelper h) {
        var caster = h.spawn(EntityType.COW, 1, 2, 1); caster.setNoAi(true); caster.setNoGravity(true);
        var target = h.spawn(EntityType.COW, 6, 2, 6); target.setNoAi(true); target.setNoGravity(true);
        var glyph = IronsNouveau.glyphs().stream().filter(g -> g.definition().spellId().getPath().equals("summon_swords")).findFirst().orElseThrow();
        var resolver = new SpellResolver(new SpellContext(h.getLevel(), new Spell(MethodTouch.INSTANCE, glyph), caster,
                new LivingCaster(caster) {
                    @Override public boolean enoughMana(int cost) { return true; }
                    @Override public void expendMana(int cost) {}
                }, ItemStack.EMPTY));
        glyph.onResolve(new EntityHitResult(target), h.getLevel(), caster, new SpellStats.Builder().build(), resolver.spellContext, resolver);
        var weapons = h.getLevel().getEntitiesOfClass(SummonedWeaponEntity.class, target.getBoundingBox().inflate(8),
                e -> SummonManager.getOwner(e) == caster);
        h.assertTrue(weapons.size() == 3, "One activation creates exactly three weapons: " + weapons.size());
        for (var weapon : weapons) {
            weapon.setNoAi(true); weapon.setNoGravity(true);
            var tag = weapon.getPersistentData().getCompound("irons_nouveau_lease");
            h.assertTrue(tag.getLong("expires") > h.getLevel().getGameTime(), "Every actual weapon has a lifetime");
            tag.putLong("expires", h.getLevel().getGameTime() + 8);
        }
        var nativeRemoved = weapons.getFirst();
        ((IMagicSummon)nativeRemoved).onUnSummon();
        h.runAtTickTime(3, () -> {
            h.assertTrue(nativeRemoved.isRemoved() && h.getLevel().getEntity(nativeRemoved.getUUID()) == null,
                    "Native unsummon removes that exact entity");
            h.assertTrue(weapons.stream().skip(1).noneMatch(e -> e.isRemoved()), "Other members remain until their own expiry");
        });
        h.runAtTickTime(12, () -> {
            for (var weapon : weapons) {
                h.assertTrue(weapon.isRemoved() && h.getLevel().getEntity(weapon.getUUID()) == null,
                        "Expired entity cannot remain active: " + weapon.getUUID());
                h.assertTrue(!SummonManager.getSummons(caster).contains(weapon.getUUID()), "Removed entity leaves summon registry");
            }
            h.succeed();
        });
    }
}
