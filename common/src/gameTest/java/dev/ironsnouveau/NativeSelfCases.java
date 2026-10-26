package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.resource.ResourceAccount;
import dev.ironsnouveau.bridge.*;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.compat.*;
import dev.ironsnouveau.glyph.BridgeGlyph;
import io.redspace.ironsspellbooks.api.magic.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.gametest.framework.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.*;
import net.minecraft.world.item.ItemStack;
import java.util.*;

public final class NativeSelfCases {
    public static void online(ServerPlayer player,Runnable test) {
        try {
            var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
            @SuppressWarnings("unchecked") var players=(Map<UUID,ServerPlayer>)field.get(player.server.getPlayerList());
            var previous=players.put(player.getUUID(),player);
            try {test.run();} finally {if(previous==null)players.remove(player.getUUID());else players.put(player.getUUID(),previous);}
        } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
    }
    static final class Account implements ResourceAccount {
        int mana;Account(int mana) {this.mana=mana;}
        public Object reservationKey(){return this;}public boolean canSpend(int amount){return amount<=mana;}
        public void spend(int amount){mana-=amount;}
    }
    static BridgeGlyph glyph(String name) {
        return (BridgeGlyph)GlyphRegistry.getSpellPart(NativeSpellIndex.glyphId(dev.ironsnouveau.platform.Locations.id("native_fixture:"+name)));
    }
    private static SpellContext context(ServerPlayer p,BridgeGlyph glyph,Account account) {
        var context=SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,glyph),p,ItemStack.EMPTY);
        CastContexts.bind(context,CastContext.of(context,new EntityHitResult(p),account));return context;
    }
    private static void fire(ServerPlayer p,BridgeGlyph glyph,Account account,HitResult hit) {
        var context=context(p,glyph,account);
        glyph.onResolve(hit,p.level(),p,new SpellStats.Builder().build(),context,new SpellResolver(context));
    }
    public static void registration(GameTestHelper h) {
        var instant=glyph("instant");
        h.assertTrue(instant!=null&&instant.nativeSelf(),"Standard addon DeferredRegister produces a native glyph");
        h.assertTrue(dev.ironsnouveau.compat.NativeGlyphResources.model(dev.ironsnouveau.platform.Locations.id("irons_nouveau:item/glyph_native/native_fixture/instant"))!=null,"Native icon also has a generated item model");
        h.assertTrue(!instant.compatibleAugments.contains(AugmentAOE.INSTANCE),"Unsupported area augment is not offered");
        h.assertTrue(!NativeCasting.validate(List.of(MethodProjectile.INSTANCE,instant)).isEmpty(),"Projectile generic casting is rejected by recipe validation");
        h.assertTrue(NativeCasting.validate(List.of(MethodSelf.INSTANCE,instant)).isEmpty(),"Self generic casting validates");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(instant.getRegistryName()).isPresent(),"Real scribe glyph recipe is loaded");
        h.assertTrue(IronsNouveau.forms().size()==19&&IronsNouveau.glyphs().stream().filter(g->!g.nativeSelf()).count()==84,"Explicit native catalog is unchanged");
        h.assertTrue(IronsNouveau.glyphs().stream().filter(g->g.nativeSelf()).count()==3,"Only the three external fixtures are imported; ANS proxies and base spells are excluded");
        h.succeed();
    }
    public static void instantAndPreconditions(GameTestHelper h,ServerPlayer p) {
        var glyph=glyph("instant");var account=new Account(30);var data=MagicData.getPlayerMagicData(p);data.setMana(0);
        p.getPersistentData().putBoolean("fixture_reject",true);fire(p,glyph,account,new EntityHitResult(p));
        h.assertTrue(account.mana==30&&!data.isCasting(),"Failed native precondition costs nothing");
        p.getPersistentData().putBoolean("fixture_reject",false);
        fire(p,glyph,account,BlockHitResult.miss(p.position(),net.minecraft.core.Direction.UP,p.blockPosition()));
        h.assertTrue(account.mana==30,"Non-self hit never falls back to the caster");
        fire(p,glyph,account,new EntityHitResult(p));
        h.assertTrue(p.getPersistentData().getInt("fixture_calls")==1&&account.mana==23&&data.getMana()==0,"Native effect fires once and only the selected Conflux account pays: calls="+p.getPersistentData().getInt("fixture_calls")+", account="+account.mana+", native="+data.getMana());
        h.assertTrue(p.getPersistentData().getInt("fixture_pre")==1&&p.getPersistentData().getInt("fixture_complete")==1&&!data.isCasting(),"Native pre-cast and completion both execute");
        h.assertTrue(data.getPlayerCooldowns().isOnCooldown(SpellRegistry.getSpell(glyph.definition().spellId())),"Native cooldown is shared");
        var shots=h.getLevel().getEntitiesOfClass(io.redspace.ironsspellbooks.entity.spells.firebolt.FireboltProjectile.class,p.getBoundingBox().inflate(10));
        h.assertTrue(shots.stream().anyMatch(s->s.getOwner()==p),"Native projectile keeps the real caster");
        h.succeed();
    }
    public static void preparedAndNativePool(GameTestHelper h,ServerPlayer p) {
        var glyph=glyph("long");var data=MagicData.getPlayerMagicData(p);data.setMana(30);
        // A null account takes the production Iron/Ars pool selection route.
        var context=SpellContext.fromEntity(new Spell(MethodSelf.INSTANCE,glyph),p,ItemStack.EMPTY);
        glyph.onResolve(new EntityHitResult(p),p.level(),p,new SpellStats.Builder().build(),context,new SpellResolver(context));
        h.assertTrue(p.getPersistentData().getInt("fixture_calls")==1&&!data.isCasting(),"Prepared long spell does not chant a second time");
        h.assertTrue(data.getMana()==23,"Production Iron pool is charged exactly once: "+data.getMana());h.succeed();
    }
    public static void untouchedNativeCast(GameTestHelper h,ServerPlayer p) {
        var spell=SpellRegistry.FIREBOLT_SPELL.get();var data=MagicData.getPlayerMagicData(p);data.setMana(80);
        var players=p.server.getPlayerList();
        try {
            // ANS' transaction ledger resolves a real online UUID. This fixture
            // deliberately avoids simulating the client handshake/login packets.
            var field=net.minecraft.server.players.PlayerList.class.getDeclaredField("playersByUUID");field.setAccessible(true);
            @SuppressWarnings("unchecked") var byId=(Map<UUID,ServerPlayer>)field.get(players);byId.put(p.getUUID(),p);
            try {
                spell.castSpell(h.getLevel(),1,p,io.redspace.ironsspellbooks.api.spells.CastSource.SPELLBOOK,true);
                h.assertTrue(data.getMana()==80-spell.getManaCost(1),"Native Iron casting still pays its own pool without an opt-in lease: "+data.getMana());
                h.assertTrue(data.getPlayerCooldowns().isOnCooldown(spell),"Native Iron cooldown behavior is unchanged");
            } finally {byId.remove(p.getUUID());}
        } catch(ReflectiveOperationException error) {throw new AssertionError(error);}
        h.succeed();
    }
    public static void channel(GameTestHelper h,ServerPlayer p) {
        var glyph=glyph("channel");var account=new Account(14);var data=MagicData.getPlayerMagicData(p);data.setMana(0);
        h.getLevel().addNewPlayer(p);
        try {
            fire(p,glyph,account,new EntityHitResult(p));
            h.assertTrue(data.isCasting()&&account.mana==7,"Native channel starts and pays its first pulse: casting="+data.isCasting()+", account="+account.mana);
            for(int i=0;i<25&&data.isCasting();i++){((io.redspace.ironsspellbooks.capabilities.magic.MagicManager)MagicHelper.MAGIC_MANAGER).tick(h.getLevel());NativeSelfCasts.tick();}
            h.assertTrue(account.mana==0&&p.getPersistentData().getInt("fixture_calls")==2,"Channel pulses use the original account with no Iron-pool duplicate charge");
            h.assertTrue(!data.isCasting()&&p.getPersistentData().getBoolean("fixture_cancelled"),"Insufficient mana cancels and cleans up the native channel");
            h.assertTrue(p.getPersistentData().getInt("fixture_ticks")>0,"Native per-tick callbacks run");
        } finally {h.getLevel().removePlayerImmediately(p,net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);NativeSelfCasts.stop();}
        h.succeed();
    }
}
