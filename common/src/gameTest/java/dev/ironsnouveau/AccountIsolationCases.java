package dev.ironsnouveau;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodSelf;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.resource.*;
import dev.ironsnouveau.config.SpellLevelConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

public final class AccountIsolationCases {
    public static void verify(GameTestHelper h,ServerPlayer p,java.util.function.IntConsumer setArs,java.util.function.DoubleSupplier ars) {
        var glyph=IronsNouveau.glyphs().stream().filter(g->g.definition().spellId().getPath().equals("heal")).findFirst().orElseThrow();
        boolean old=SpellLevelConfig.USE_IRON_MANA.get();SpellLevelConfig.USE_IRON_MANA.set(true);
        try {
            setArs.accept(500);var iron=MagicData.getPlayerMagicData(p);iron.setMana(500);p.setHealth(1);
            var context=new SpellContext(h.getLevel(),new Spell(MethodSelf.INSTANCE,glyph),p,LivingCaster.from(p),ItemStack.EMPTY);
            glyph.onResolve(new EntityHitResult(p),h.getLevel(),p,new SpellStats.Builder().build(),context,new SpellResolver(context));
            h.assertTrue(CastContext.of(context,null,null).account()==null,"An Iron effect never publishes its local pool as a source account");
            h.assertTrue(p.getHealth()>1 && iron.getMana()<500,"Real Iron healing uses the separate native pool");
            float after=iron.getMana();
            var next=CastContext.of(context,null,null);
            var account=next.account()!=null?next.account():new ArsManaAccount(context.getCaster());
            h.assertTrue(new ResourceCharge(account,40).trigger(()->true),"Another provider can pay its own default pool");
            h.assertTrue(ars.getAsDouble()==460 && iron.getMana()==after,"The continuation charges Ars, not the preceding Iron pool");
            var explicit=new ResourceAccount(){public Object reservationKey(){return this;}public boolean canSpend(int amount){return true;}public void spend(int amount){}};
            CastContexts.bind(context,CastContext.of(context,null,explicit));p.setHealth(1);
            glyph.onResolve(new EntityHitResult(p),h.getLevel(),p,new SpellStats.Builder().build(),context,new SpellResolver(context));
            h.assertTrue(CastContext.of(context,null,null).account()==explicit,"Explicit source payment survives across providers");
            h.succeed();
        } finally {SpellLevelConfig.USE_IRON_MANA.set(old);}
    }
}
