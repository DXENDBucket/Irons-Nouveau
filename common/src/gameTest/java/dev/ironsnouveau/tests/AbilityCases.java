package dev.ironsnouveau.tests;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.resource.ResourceAccount;
import dev.ironsnouveau.ability.*;
import io.redspace.ironsspellbooks.capabilities.magic.SummonManager;
import io.redspace.ironsspellbooks.entity.mobs.dead_king_boss.undead_spawner.UndeadRiftEntity;
import io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity;
import io.redspace.ironsspellbooks.entity.spells.magma_ball.FireField;
import io.redspace.ironsspellbooks.entity.spells.wall_of_fire.WallOfFireEntity;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import java.util.concurrent.atomic.*;

public final class AbilityCases {
    private static LivingEntity actor(GameTestHelper h,int x,int z){var e=h.spawn(EntityType.COW,x,2,z);e.setNoAi(true);e.setNoGravity(true);return e;}
    private static AbilityGlyph glyph(IronAbility a){return (AbilityGlyph)AbilityCatalog.parts().stream().filter(p->IronAbility.of(p)==a).findFirst().orElseThrow();}
    private static SpellContext context(GameTestHelper h,LivingEntity caster,Spell spell,AtomicInteger mana){
        var account=new ResourceAccount(){public Object reservationKey(){return mana;}public boolean canSpend(int n){return mana.get()>=n;}public void spend(int n){mana.addAndGet(-n);}};
        var source=new LivingCaster(caster){public boolean enoughMana(int n){return mana.get()>=n;}public void expendMana(int n){mana.addAndGet(-n);}};
        var context=new SpellContext(h.getLevel(),spell,caster,source,ItemStack.EMPTY);
        CastContexts.bind(context,CastContext.of(context,null,account));return context;
    }
    private static void cast(GameTestHelper h,LivingEntity caster,IronAbility a,AtomicInteger mana,HitResult hit){
        var part=glyph(a);var context=context(h,caster,new Spell(MethodTouch.INSTANCE,part),mana);
        part.onResolve(hit,h.getLevel(),caster,new SpellStats.Builder().build(),context,new SpellResolver(context));
    }
    public static void registered(GameTestHelper h){
        for(var a:IronAbility.values()){
            var part=com.hollingsworth.arsnouveau.api.registry.GlyphRegistry.getSpellPart(a.glyphId());
            h.assertTrue(part!=null,"Registered "+a);
            h.assertTrue(h.getLevel().getRecipeManager().byKey(a.glyphId()).isPresent(),"Real recipe "+a);
            h.assertTrue(!dev.arsconflux.api.glyph.GlyphAbilities.get(part).isEmpty(),"AI hint "+a);
        }
        var caster=actor(h,2,2);var mana=new AtomicInteger(0);
        cast(h,caster,IronAbility.UNDEAD_RIFT,mana,new EntityHitResult(caster));
        h.assertTrue(h.getLevel().getEntitiesOfClass(UndeadRiftEntity.class,caster.getBoundingBox().inflate(16)).isEmpty(),"No free rift at zero mana");h.succeed();
    }
    public static void rift(GameTestHelper h){
        var caster=actor(h,2,2);var mana=new AtomicInteger(1000);
        cast(h,caster,IronAbility.UNDEAD_RIFT,mana,new EntityHitResult(caster));
        h.assertTrue(mana.get()==700,"Rift creation pays once");
        h.runAtTickTime(132,()->{
            long mobs=SummonManager.getSummons(caster).stream().map(id->h.getLevel().getEntity(id)).filter(e->e instanceof Mob).count();
            h.assertTrue(mobs==5,"Native rift produced five owned undead, got "+mobs);
            cast(h,caster,IronAbility.UNDEAD_RIFT,mana,new EntityHitResult(caster));
            cast(h,caster,IronAbility.UNDEAD_RIFT,mana,new EntityHitResult(caster));
        });
        h.runAtTickTime(270,()->{
            long mobs=SummonManager.getSummons(caster).stream().map(id->h.getLevel().getEntity(id)).filter(e->e instanceof Mob).count();
            h.assertTrue(mobs==12,"Concurrent rifts stop at twelve summons, got "+mobs);
            caster.discard();
        });
        h.runAtTickTime(273,()->{
            h.assertTrue(SummonManager.getSummons(caster).isEmpty(),"Owner loss cleans native summons and registry");h.succeed();
        });
    }
    public static void zone(GameTestHelper h){
        var caster=actor(h,2,2);var mana=new AtomicInteger(1000);
        cast(h,caster,IronAbility.DAGGER_ZONE,mana,new EntityHitResult(caster));
        var fields=h.getLevel().getEntitiesOfClass(FireField.class,caster.getBoundingBox().inflate(16));
        h.assertTrue(fields.size()==1&&fields.get(0).getOwner()==caster,"Native zone field belongs to real caster without any Boss nearby");
        h.assertTrue(h.getLevel().getEntitiesOfClass(FieryDaggerEntity.class,caster.getBoundingBox().inflate(16)).size()>20,"Native rising blades retained");
        h.assertTrue(mana.get()==820,"Zone is paid once, child creation is prepaid");h.succeed();
    }
    public static void wall(GameTestHelper h){
        var caster=actor(h,2,2);var target=actor(h,7,2);var mana=new AtomicInteger(100);
        cast(h,caster,IronAbility.RING_OF_FIRE,mana,new EntityHitResult(caster));
        var walls=h.getLevel().getEntitiesOfClass(WallOfFireEntity.class,caster.getBoundingBox().inflate(16));
        h.assertTrue(walls.size()==1&&walls.get(0).getParts().length>16,"Native multipart ring was constructed");
        h.runAtTickTime(3,()->{
            h.assertTrue(walls.get(0).isRemoved(),"No mana for ongoing pulse removes field");
            h.assertTrue(target.getHealth()==target.getMaxHealth(),"Unpaid pulse causes no damage");h.succeed();
        });
    }
    public static void gust(GameTestHelper h){
        var caster=actor(h,2,2);var ally=actor(h,3,2);var enemy=actor(h,4,2);
        var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("gust_"+caster.getId());
        board.addPlayerToTeam(caster.getStringUUID(),team);board.addPlayerToTeam(ally.getStringUUID(),team);
        var mana=new AtomicInteger(25);
        cast(h,caster,IronAbility.EXPULSION,mana,new EntityHitResult(caster));
        h.assertTrue(enemy.getDeltaMovement().lengthSqr()>.01,"Enemy receives impulse");
        h.assertTrue(ally.getDeltaMovement().lengthSqr()<.0001&&caster.getDeltaMovement().lengthSqr()<.0001,"Allies and caster are unaffected");
        h.assertTrue(mana.get()==0,"Gust charged once");h.succeed();
    }
    public static void dagger(GameTestHelper h){
        var caster=actor(h,2,2);var enemy=actor(h,7,2);enemy.setHealth(enemy.getMaxHealth());caster.setYRot(-90);
        var continued=new AtomicBoolean();
        var probe=new AbstractEffect(dev.ironsnouveau.platform.Locations.id("irons_nouveau_test:ability_probe"),"Probe"){
            public void onResolve(HitResult hit,net.minecraft.world.level.Level w,LivingEntity c,SpellStats s,SpellContext ctx,SpellResolver r){continued.set(true);}
            public int getDefaultManaCost(){return 0;}public SpellTier defaultTier(){return SpellTier.ONE;}
            @Override public boolean isEnabled(){return true;}
            protected java.util.Set<AbstractAugment> getCompatibleAugments(){return java.util.Set.of();}
        };
        var spell=new Spell(MethodProjectile.INSTANCE,AbilityCatalog.DAGGER,probe);var mana=new AtomicInteger(1000);
        var context=context(h,caster,spell,mana);new SpellResolver(context).onCast(ItemStack.EMPTY,h.getLevel());
        h.runAtTickTime(60,()->{
            h.assertTrue(enemy.getHealth()<enemy.getMaxHealth(),"Native fiery dagger hit target");
            h.assertTrue(continued.get(),"Successful native damage resumes the remaining Ars effect");
            h.assertTrue(mana.get()<1000,"Projectile creation paid mana");h.succeed();
        });
    }
    private AbilityCases(){}
}
