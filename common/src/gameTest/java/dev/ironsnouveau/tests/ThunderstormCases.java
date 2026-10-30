package dev.ironsnouveau.tests;

import com.hollingsworth.arsnouveau.api.spell.SpellStats;
import dev.arsconflux.api.resource.*;
import dev.arsconflux.api.targeting.Allies;
import dev.ironsnouveau.IronsNouveau;
import dev.ironsnouveau.bridge.Resolution;
import dev.ironsnouveau.bridge.WorldSpellAdapters;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.AoeEntity;
import io.redspace.ironsspellbooks.entity.spells.LightningStrike;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import java.util.*;

public final class ThunderstormCases {
    private static LivingEntity cow(GameTestHelper h,int x,int z) {
        return actor(h,EntityType.COW,x,z);
    }
    private static LivingEntity actor(GameTestHelper h,EntityType<? extends Mob> type,int x,int z) {
        for(int dx=0;dx<16;dx++)for(int dz=0;dz<16;dz++)for(int dy=7;dy<=11;dy++)h.setBlock(dx,dy,dz,net.minecraft.world.level.block.Blocks.AIR);
        var actor=h.spawn(type,x,8,z);actor.setNoAi(true);actor.setNoGravity(true);return actor;
    }
    public static void transferredStormKeepsCasterAndNativeStrike(GameTestHelper h) throws Exception {
        var caster=actor(h,EntityType.PILLAGER,5,8);var host=cow(h,2,8);var enemy=actor(h,EntityType.HUSK,7,8);var ally=actor(h,EntityType.HUSK,3,13);
        MobMana.configure(caster,new MobMana.Profile(100000,0));var pool=ArsManaPools.of(caster);
        var glyph=IronsNouveau.glyphs().stream().filter(g->g.definition().spellId().getPath().equals("thunderstorm")).findFirst().orElseThrow();
        var spell=SpellRegistry.getSpell(glyph.definition().spellId());
        try(var relation=Allies.register(dev.ironsnouveau.platform.Locations.id("irons_nouveau:test_storm_allies"),(a,b)->a==caster&&b==ally)) {
            h.assertTrue(WorldSpellAdapters.THUNDERSTORM.apply(new Resolution(h.getLevel(),caster,spell,glyph.definition(),new SpellStats.Builder().build()),host),"Native storm status attaches to selected entity");
            MobEffectRegistry.THUNDERSTORM.get().applyEffectTick(host,8);
        }
        var bolts=h.getLevel().getEntitiesOfClass(LightningStrike.class,host.getBoundingBox().inflate(20),b->b.getOwner()==caster);
        h.assertTrue(!bolts.isEmpty()&&bolts.stream().anyMatch(b->b.distanceToSqr(enemy)<.1),"Native lightning is created at hostile target and credits original caster: "+details(h,host,enemy));
        h.assertTrue(bolts.stream().noneMatch(b->b.distanceToSqr(caster)<.1||b.distanceToSqr(ally)<.1||b.distanceToSqr(host)<.1),"Caster, host and Core-only ally are not chosen as strike centers");
        h.assertTrue(pool.current()==pool.maximum()-spell.getManaCost(1),"One native upkeep price belongs to original caster");
        float casterHealth=caster.getHealth(),enemyHealth=enemy.getHealth();
        var hits=AoeEntity.class.getDeclaredMethod("checkHits");hits.setAccessible(true);
        for(var bolt:bolts)hits.invoke(bolt);
        h.assertTrue(caster.getHealth()==casterHealth&&enemy.getHealth()<enemyHealth,"Native AoE excludes original caster even next to struck enemy");
        h.assertTrue(enemy.getLastHurtByMob()==caster,"Native damage owner is actual caster");
        for(var bolt:bolts)bolt.discard();pool.set(0);
        MobEffectRegistry.THUNDERSTORM.get().applyEffectTick(host,8);
        h.assertTrue(h.getLevel().getEntitiesOfClass(LightningStrike.class,host.getBoundingBox().inflate(20),b->b.getOwner()==caster).isEmpty(),"Empty caster pool stops pulse without a free lightning strike");
        caster.discard();host.discard();enemy.discard();ally.discard();h.succeed();
    }
    public static void nativeStormWithoutBridgeMetadataStaysNative(GameTestHelper h) {
        var host=cow(h,2,8);var target=actor(h,EntityType.HUSK,7,8);
        MobEffectRegistry.THUNDERSTORM.get().applyEffectTick(host,8);
        var bolts=h.getLevel().getEntitiesOfClass(LightningStrike.class,host.getBoundingBox().inflate(20),b->b.getOwner()==host);
        h.assertTrue(bolts.stream().anyMatch(b->b.distanceToSqr(target)<.1)&&bolts.stream().noneMatch(b->b.distanceToSqr(host)<.1),"Unmanaged native Iron storm retains host as owner and excludes it: "+details(h,host,target));
        for(var bolt:bolts)bolt.discard();host.discard();target.discard();h.succeed();
    }
    private static String details(GameTestHelper h,LivingEntity host,LivingEntity target) {
        return "visible="+io.redspace.ironsspellbooks.api.util.Utils.hasLineOfSight(h.getLevel(),host,target,false)
                +", pickable="+target.isPickable()+", heal="+io.redspace.ironsspellbooks.api.util.Utils.shouldHealEntity(host,target)
                +", bolts="+h.getLevel().getEntitiesOfClass(LightningStrike.class,host.getBoundingBox().inflate(30)).stream().map(b->b.position()+"/"+b.getOwner()).toList();
    }
    private ThunderstormCases() {}
}
