package dev.ironsnouveau;
import com.hollingsworth.arsnouveau.api.registry.GlyphRegistry;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.*;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.resource.ResourceAccount;
import dev.ironsnouveau.casting.*;
import dev.ironsnouveau.compat.NativeSpellIndex;
import dev.ironsnouveau.compat.wind.*;
import dev.ironsnouveau.glyph.*;
import dev.ironsnouveau.platform.Locations;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class WindManualCases {
    private static LivingEntity cow(GameTestHelper h,int x,int z){var e=h.spawn(EntityType.COW,x,3,z);e.setNoAi(true);e.setNoGravity(true);return e;}
    private static AbstractSpellPart part(String path){return GlyphRegistry.getSpellPart(NativeSpellIndex.glyphId(Locations.id("wind_spellbooks",path)));}
    private static ResourceAccount account(AtomicInteger balance){return new ResourceAccount(){
        public Object reservationKey(){return balance;}public boolean canSpend(int n){return balance.get()>=n;}public void spend(int n){balance.addAndGet(-n);}};}
    private static SpellContext context(GameTestHelper h,LivingEntity caster,Spell spell,AtomicInteger balance){
        var source=new LivingCaster(caster){
            public boolean enoughMana(int amount){return balance.get()>=amount;}
            public void expendMana(int amount){balance.addAndGet(-amount);}
        };
        var ctx=new SpellContext(h.getLevel(),spell,caster,source,ItemStack.EMPTY);
        CastContexts.bind(ctx,CastContext.of(ctx,null,account(balance)));return ctx;
    }
    private static void cast(GameTestHelper h,LivingEntity caster,String path,HitResult hit,AtomicInteger balance){
        var glyph=(BridgeGlyph)part(path);var ctx=context(h,caster,new Spell(MethodTouch.INSTANCE,glyph),balance);
        glyph.onResolve(hit,h.getLevel(),caster,new SpellStats.Builder().build(),ctx,new SpellResolver(ctx));
    }
    private static void allies(GameTestHelper h,LivingEntity a,LivingEntity b){
        var board=h.getLevel().getScoreboard();var name="wind"+a.getId();var team=board.addPlayerTeam(name);
        board.addPlayerToTeam(a.getStringUUID(),team);board.addPlayerToTeam(b.getStringUUID(),team);
    }
    public static void registration(GameTestHelper h){
        for(var path:WindManualCatalog.SPELLS){
            var part=part(path);h.assertTrue(part!=null,"Manual glyph registered: "+path);
            h.assertTrue(!(part instanceof AbstractBridgeGlyph g&&g.nativeSelf()),"No self-only fallback remains: "+path);
            h.assertTrue(h.getLevel().getRecipeManager().byKey(part.getRegistryName()).isPresent(),"Actual scroll recipe: "+path);
            h.assertTrue(!dev.arsconflux.api.glyph.GlyphAbilities.get(part).isEmpty(),"Instinct hint: "+path);
        }
        h.assertTrue(part("wind_blade") instanceof NativeFormAugment,"Wind Blade is a projectile replacement");
        h.assertTrue(NativeCasting.validate(java.util.List.of(MethodProjectile.INSTANCE,part("wind_blade"))).isEmpty(),"Projectile combination accepted");
        h.succeed();
    }
    public static void tornado(GameTestHelper h){
        var caster=cow(h,1,1);var mana=new AtomicInteger(10000);var point=h.absoluteVec(new Vec3(9,3,9));
        var hit=new BlockHitResult(point,net.minecraft.core.Direction.UP,net.minecraft.core.BlockPos.containing(point),false);
        cast(h,caster,"tornado",hit,mana);
        var list=h.getLevel().getEntitiesOfClass(net.raptorzizi.wind_spellbooks.entity.spells.tornado.TornadoEntity.class,new AABB(point,point).inflate(8));
        h.assertTrue(list.size()==1,"A single native tornado appears at the remote point");var tornado=list.get(0);
        h.assertTrue(tornado.position().distanceTo(point)<.01&&tornado.getOwner()==caster,"Remote placement retains owner");
        h.assertTrue(tornado.getDamage()>0&&tornado.getRadius()>5&&tornado.getSyncedDuration()>0,"Native power, radius and duration configured");
        h.assertTrue(EffectResources.managed(tornado),"Wind payload has a real lifecycle lease");
        int cost=SpellRegistry.getSpell(Locations.id("wind_spellbooks:tornado")).getManaCost(1);
        h.assertTrue(mana.get()==10000-cost,"Tornado pays native cost once");caster.discard();
        h.runAfterDelay(2,()->{h.assertTrue(tornado.isRemoved(),"Owner loss removes the area");h.succeed();});
    }
    public static void push(GameTestHelper h){
        var caster=cow(h,1,1);var mana=new AtomicInteger(10000);var point=h.absoluteVec(new Vec3(8,4,8));
        cast(h,caster,"almighty_push",new BlockHitResult(point,net.minecraft.core.Direction.UP,net.minecraft.core.BlockPos.containing(point),false),mana);
        var list=h.getLevel().getEntitiesOfClass(net.raptorzizi.wind_spellbooks.entity.spells.almighty_push.AlmightyPushEntity.class,new AABB(point,point).inflate(8));
        h.assertTrue(list.size()==1,"Native multipart push entity spawned");var push=list.get(0);
        h.assertTrue(push.position().add(0,push.getRadius(),0).distanceTo(point)<.01,"Sphere center follows selected point, not owner");
        h.assertTrue(push.getOwner()==caster&&push.strength>0,"Push retains owner and native strength");h.succeed();
    }
    public static void tailwind(GameTestHelper h){
        var caster=cow(h,1,1);var ally=cow(h,5,5);var enemy=cow(h,8,8);allies(h,caster,ally);var mana=new AtomicInteger(10000);
        cast(h,caster,"tailwind",new EntityHitResult(ally),mana);
        h.assertTrue(!ally.getActiveEffects().isEmpty()&&caster.getActiveEffects().isEmpty(),"Buff is on selected ally");
        int paid=mana.get();cast(h,caster,"tailwind",new EntityHitResult(enemy),mana);
        h.assertTrue(mana.get()==paid&&enemy.getActiveEffects().isEmpty(),"Hostile recipient neither receives buff nor pays");h.succeed();
    }
    public static void jump(GameTestHelper h){
        var caster=cow(h,1,1);var ally=cow(h,5,5);allies(h,caster,ally);ally.setYHeadRot(-90);ally.setYRot(0);var mana=new AtomicInteger(10000);
        cast(h,caster,"wind_jump",new EntityHitResult(ally),mana);
        h.assertTrue(ally.getDeltaMovement().x>0&&ally.getDeltaMovement().y>0,"Actual actor head view determines impulse");
        h.assertTrue(caster.getDeltaMovement().lengthSqr()==0,"Payment owner is not moved");
        h.assertTrue(!ally.getActiveEffects().isEmpty(),"Native slow falling retained");h.succeed();
    }
    public static void actions(GameTestHelper h){
        var caster=cow(h,1,1);var ally=cow(h,6,6);allies(h,caster,ally);ally.setYHeadRot(-90);ally.setYRot(0);var mana=new AtomicInteger(10000);
        cast(h,caster,"iron_slash",new EntityHitResult(ally),mana);
        var slashes=h.getLevel().getEntitiesOfClass(net.raptorzizi.wind_spellbooks.entity.spells.iron_slash.IronSlashEntity.class,ally.getBoundingBox().inflate(24));
        h.assertTrue(slashes.size()==1,"Native delayed slash and renderer payload retained");
        h.assertTrue(slashes.get(0).getEnd().x>slashes.get(0).getStart().x,"Slash follows ally view");
        cast(h,caster,"aeropic",new EntityHitResult(ally),mana);
        h.assertTrue(ally.getDeltaMovement().x>0,"Native aerial lunge follows selected actor view");
        h.assertTrue(ally.getActiveEffects().stream().anyMatch(e->e.getAmplifier()>0),"Native acrobatics effect retained");h.succeed();
    }
    public static void blade(GameTestHelper h){
        var caster=cow(h,2,2);var target=cow(h,7,2);caster.setYHeadRot(-90);caster.setYRot(-90);
        var hits=new AtomicInteger();var mana=new AtomicInteger(10000);
        var probe=new AbstractEffect(Locations.id("irons_nouveau_test:wind_probe"),"Probe"){
            public void onResolve(HitResult hit,net.minecraft.world.level.Level world,LivingEntity owner,SpellStats stats,SpellContext ctx,SpellResolver resolver){if(hit instanceof EntityHitResult e&&e.getEntity()==target)hits.incrementAndGet();}
            public int getDefaultManaCost(){return 0;}public SpellTier defaultTier(){return SpellTier.ONE;}public boolean isEnabled(){return true;}
            protected java.util.Set<AbstractAugment> getCompatibleAugments(){return java.util.Set.of();}
        };
        var ctx=context(h,caster,new Spell(MethodProjectile.INSTANCE,part("wind_blade"),probe),mana);
        new SpellResolver(ctx).onCast(ItemStack.EMPTY,h.getLevel());
        h.runAfterDelay(12,()->{
            h.assertTrue(target.getHealth()<target.getMaxHealth(),"Native Wind Blade damages the target");
            h.assertTrue(hits.get()==1,"Successful native damage resumes remaining Ars effects exactly once");
            h.assertTrue(mana.get()<10000,"Blade creation is charged");h.succeed();
        });
    }
    public static void orbit(GameTestHelper h){
        var caster=cow(h,4,4);var mana=new AtomicInteger(10000);
        var spell=new Spell(MethodSelf.INSTANCE,com.hollingsworth.arsnouveau.common.spell.effect.EffectOrbit.INSTANCE,part("wind_blade"));
        var ctx=context(h,caster,spell,mana);new SpellResolver(ctx).onCast(ItemStack.EMPTY,h.getLevel());
        var blades=h.getLevel().getEntitiesOfClass(net.raptorzizi.wind_spellbooks.entity.spells.wind_blade.WindBladeProjectile.class,caster.getBoundingBox().inflate(8));
        h.assertTrue(!blades.isEmpty(),"Orbit creates native Wind Blade visuals");
        h.assertTrue(blades.stream().allMatch(b->((WindBladeState)b).ironsNouveau$arsDriven()),"Ars owns orbit motion");
        h.runAfterDelay(60,()->{
            for(var blade:blades){
                h.assertTrue(!blade.isRemoved(),"Ars orbit is not killed by native 50-tick expiry");
                var state=(WindBladeState)blade;
                h.assertTrue(state.confluxTrajectory()!=null&&state.confluxTrajectory().position().distanceTo(blade.position())<.01,"One visual follows original Ars trajectory");
            }
            h.succeed();
        });
    }
    public static void splash(GameTestHelper h){
        var method=CarrierProfiles.SPLASH.glyph();
        h.assertTrue(method!=null,"NEG Splash form is installed in the coexistence run");
        var caster=cow(h,1,1);caster.setYRot(-90);caster.setYHeadRot(-90);
        var a=cow(h,6,1);var b=cow(h,6,4);var mana=new AtomicInteger(10000);
        var spell=new Spell(method,part("wind_blade"),
                com.hollingsworth.arsnouveau.common.spell.augment.AugmentAOE.INSTANCE,
                com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown.INSTANCE,
                com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown.INSTANCE,
                com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown.INSTANCE,
                com.hollingsworth.arsnouveau.common.spell.augment.AugmentDurationDown.INSTANCE);
        h.assertTrue(new SpellResolver(context(h,caster,spell,mana)).onCast(ItemStack.EMPTY,h.getLevel()),"NEG splash initial cast succeeds");
        h.succeedWhen(()->h.assertTrue(a.getHealth()<a.getMaxHealth()&&b.getHealth()<b.getMaxHealth(),"NEG splash damage: a="+a.getHealth()+", b="+b.getHealth()+", mana="+mana.get()+", bladeCount="+h.getLevel().getEntitiesOfClass(net.raptorzizi.wind_spellbooks.entity.spells.wind_blade.WindBladeProjectile.class,caster.getBoundingBox().inflate(32)).size()));
    }
    private WindManualCases(){}
}
