package dev.ironsnouveau;

import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.entity.*;
import com.hollingsworth.arsnouveau.common.spell.method.MethodProjectile;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.projectile.*;
import dev.arsconflux.api.resource.ResourceAccount;
import dev.ironsnouveau.casting.*;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import io.redspace.ironsspellbooks.entity.spells.AbstractMagicProjectile;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

/** Real form ticks/collisions, not direct calls to the provider's impact callback. */
public final class TrajectorySurvivalCases {
    private static final class Marker extends AbstractEffect {
        final List<Entity> targets=new ArrayList<>();
        Marker(){super(dev.ironsnouveau.platform.Locations.id("irons_nouveau:contact_test_marker"),"Contact marker");}
        public void onResolve(HitResult hit,Level world,LivingEntity caster,SpellStats stats,SpellContext context,SpellResolver resolver){if(hit instanceof EntityHitResult e)targets.add(e.getEntity());}
        public int getDefaultManaCost(){return 0;}public int getCastingCost(){return 0;}
        public SpellTier defaultTier(){return SpellTier.ONE;}public SpellTier getConfigTier(){return SpellTier.ONE;}
        public boolean isEnabled(){return true;}protected Set<AbstractAugment> getCompatibleAugments(){return Set.of();}
        public String getBookDescription(){return "Test continuation";}
    }
    private static final class Arena implements AutoCloseable {
        final GameTestHelper h;final Vec3 center;final LivingEntity caster;
        final List<Entity> entities=new ArrayList<>();final List<net.minecraft.world.level.ChunkPos> forced=new ArrayList<>();
        final AtomicInteger mana=new AtomicInteger(10000);final Marker marker=new Marker();
        EntityProjectileSpell carrier;AbstractMagicProjectile ball;CastSession session;
        Arena(GameTestHelper h){
            this.h=h;var p=h.absoluteVec(new Vec3(4,0,4));center=new Vec3(p.x,h.getLevel().getMaxBuildHeight()-32,p.z);
            var block=BlockPos.containing(center);
            for(int x=(block.getX()>>4)-1;x<=(block.getX()>>4)+1;x++)for(int z=(block.getZ()>>4)-1;z<=(block.getZ()>>4)+1;z++){
                if(!h.getLevel().getForcedChunks().contains(net.minecraft.world.level.ChunkPos.asLong(x,z))){h.getLevel().setChunkForced(x,z,true);forced.add(new net.minecraft.world.level.ChunkPos(x,z));}
                h.getLevel().getChunk(x,z);
            }
            caster=target(center.add(-12,0,0));
        }
        LivingEntity target(Vec3 p){
            var e=EntityType.COW.create(h.getLevel());e.setNoAi(true);e.setNoGravity(true);e.setPos(p);
            e.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);e.setHealth(1000);e.getAttribute(Attributes.KNOCKBACK_RESISTANCE).setBaseValue(1);
            h.getLevel().addFreshEntity(e);entities.add(e);return e;
        }
        void launch(String shape,String nativeSpell){
            AbstractSpellPart method=MethodProjectile.INSTANCE;
            if(shape.equals("splash"))method=Objects.requireNonNull(CarrierRegistry.get(CarrierRegistry.id("arsomega:glyph_missile")).glyph(),"NEG missile required by this focused batch");
            var glyph=IronsNouveau.forms().stream().filter(f->f.spellId().getPath().equals(nativeSpell)).findFirst().orElseThrow();
            var context=new SpellContext(h.getLevel(),new Spell(method,glyph,marker),caster,LivingCaster.from(caster),ItemStack.EMPTY);
            var account=new ResourceAccount(){
                public Object reservationKey(){return mana;}public boolean canSpend(int cost){return mana.get()>=cost;}
                public void spend(int cost){mana.addAndGet(-cost);}
            };
            CastContexts.bind(context,CastContext.of(context,null,account));
            var resolver=new SpellResolver(context);resolver.castType=(AbstractCastMethod)method;context.nextPart();
            try{
                carrier=shape.equals("orbit")?new EntityOrbitProjectile(h.getLevel(),resolver,caster)
                    :shape.equals("splash")?(EntityProjectileSpell)Class.forName("alexthw.not_enough_glyphs.common.spell.MissileProjectile").getConstructor(Level.class,SpellResolver.class).newInstance(h.getLevel(),resolver)
                    :new EntityProjectileSpell(h.getLevel(),resolver);
            }catch(ReflectiveOperationException failure){throw new AssertionError(failure);}
            carrier.setOwner(caster);carrier.setPos(shape.equals("orbit")?carrier.getNextHitPosition():center);carrier.setDeltaMovement(new Vec3(.8,0,0));
            h.assertTrue(NativeCasting.convert(carrier),"Real provider conversion");
            ball=h.getLevel().getEntitiesOfClass(AbstractMagicProjectile.class,new AABB(center,center).inflate(32),p->((NativeCastCarrier)p).ironsNouveau$session()!=null).stream().findFirst().orElseThrow();
            entities.add(ball);session=((NativeCastCarrier)ball).ironsNouveau$session();
        }
        void tick(){session.tick();h.assertTrue(TrajectoryContacts.current(carrier)==null,"Contact scope closes after the actual form tick");}
        void collide(){for(int i=0;i<12 && marker.targets.isEmpty() && session.active();i++)tick();}
        void rewindForContact(){carrier.setPos(center);carrier.setDeltaMovement(new Vec3(.8,0,0));}
        public void close(){if(session!=null)session.finish(CastSession.EndReason.SERVER_STOPPED);entities.forEach(Entity::discard);forced.forEach(c->h.getLevel().setChunkForced(c.x,c.z,false));}
    }
    private static int cost(){return SpellRegistry.getSpell(dev.ironsnouveau.platform.Locations.id("irons_spellbooks:ball_lightning")).getManaCost(1);}
    private static void near(GameTestHelper h,double actual,double expected,String why){h.assertTrue(Math.abs(actual-expected)<.001,why+": "+actual+" != "+expected);}
    public static void splashContinuesItsActualFlightAndSharesContactPayment(GameTestHelper h){
        try(var a=new Arena(h)){
            a.launch("splash","ball_lightning");int afterLaunch=a.mana.get();
            var first=a.target(a.center.add(1,0,0));var second=a.target(a.center.add(1,0,1.2));
            var original=a.carrier;a.collide();
            h.assertTrue(first.getHealth()<1000 && second.getHealth()<1000 && a.marker.targets.size()==2,"One real collision expands to both splash targets once");
            h.assertTrue(!a.ball.isRemoved() && !a.carrier.isRemoved(),"Native persistence keeps the original form alive");
            near(h,afterLaunch,10000-cost(),"Existing one-time forms still pay at launch");near(h,a.mana.get(),afterLaunch,"Launch covers the whole first splash");
            int age=a.carrier.age;Vec3 position=a.carrier.position(),velocity=a.carrier.getDeltaMovement();a.tick();
            h.assertTrue(a.carrier==original && a.carrier.age>age && a.carrier.position().distanceTo(position)>.01,"Same NEG object continues advancing after collision");
            h.assertTrue(a.carrier.getDeltaMovement().lengthSqr()>0 && a.carrier.getDeltaMovement().dot(velocity)>0,"Flight velocity survives contact");
            a.rewindForContact();first.tickCount+=10;second.tickCount+=10;first.invulnerableTime=0;second.invulnerableTime=0;
            int before=a.marker.targets.size();a.tick();
            h.assertTrue(a.marker.targets.size()==before+2,"A later physical collision expands once again");near(h,a.mana.get(),afterLaunch-cost(),"One new contact pays once, not once per splash target");
            h.succeed();
        }
    }
    public static void contactIntervalAndEmptyManaPreserveFlightWithoutFreeEffects(GameTestHelper h){
        try(var a=new Arena(h)){
            a.launch("splash","ball_lightning");var first=a.target(a.center.add(1,0,0));var second=a.target(a.center.add(1,0,1.2));a.collide();
            int resolved=a.marker.targets.size(),paid=a.mana.get();float health=first.getHealth();
            a.rewindForContact();first.invulnerableTime=0;second.invulnerableTime=0;a.tick();
            near(h,first.getHealth(),health,"Native victim interval applies even if invulnerability is cleared");near(h,a.mana.get(),paid,"Rejected contact does not pay");
            first.tickCount+=10;second.tickCount+=10;a.mana.set(0);a.rewindForContact();a.tick();
            h.assertTrue(a.marker.targets.size()==resolved && !a.ball.isRemoved(),"Empty mana denies new effects while retaining the form");
            a.mana.set(cost());a.rewindForContact();a.tick();
            h.assertTrue(a.marker.targets.size()==resolved+2,"An unpaid contact did not consume native victim eligibility");near(h,a.mana.get(),0,"One restored payment covers the splash batch");h.succeed();
        }
    }
    public static void orbitRetainsItsControllerAfterAnActualEntityContact(GameTestHelper h){
        var arena=new Arena(h);
        h.runAtTickTime(2,()->{try(var a=arena){
            a.caster.setPos(a.center);
            a.launch("orbit","ball_lightning");// Put the victim ahead of the arc, outside the initial hitbox: vanilla ray clipping ignores an entirely enclosed segment.
            var next=((EntityOrbitProjectile)a.carrier).getAngledPosition(15);var target=a.target(next.add(0,-.5,0));int before=a.mana.get();
            a.collide();h.assertTrue(target.getHealth()<1000 && !a.ball.isRemoved() && !a.carrier.isRemoved(),"Actual orbit collision retains ball and hidden orbit; health="+target.getHealth()+", removed="+a.ball.isRemoved()+"/"+a.carrier.isRemoved()+", age="+a.carrier.age+", target="+target.position()+", carrier="+a.carrier.position());
            near(h,a.mana.get(),before-cost(),"Orbit pays at successful contact");
            Vec3 pos=a.carrier.position();a.tick();h.assertTrue(a.carrier.position().distanceTo(pos)>.001,"Orbit continues turning after contact");
            a.ball.tickCount=101;a.ball.tick();h.assertTrue(!a.ball.isRemoved(),"Ars-controlled lifespan is not cut short by native free-flight age");
            a.carrier.age=a.carrier.getExpirationTime()+2;a.tick();h.assertTrue(a.ball.isRemoved(),"Form lifetime still cleans the native visual");h.succeed();
        }});
    }
    public static void wallAndOwnerRemovalRemainTerminal(GameTestHelper h){
        try(var a=new Arena(h)){
            a.launch("splash","ball_lightning");var block=BlockPos.containing(a.center.add(1,0,0));
            h.getLevel().setBlockAndUpdate(block,Blocks.STONE.defaultBlockState());
            try{for(int i=0;i<4 && a.session.active();i++)a.tick();h.assertTrue(a.ball.isRemoved(),"Entity retention does not override the form's wall contact");}
            finally{h.getLevel().setBlockAndUpdate(block,Blocks.AIR.defaultBlockState());}
        }
        try(var a=new Arena(h)){
            a.launch("orbit","ball_lightning");a.caster.discard();a.tick();h.assertTrue(a.ball.isRemoved() && a.carrier.isRemoved(),"Owner removal still closes both entities");h.succeed();
        }
    }
    /** Forge NEG has Trail but no Missile/Splash form; exercise the available native Orbit instead. */
    public static void orbitWallAndOwnerRemovalRemainTerminal(GameTestHelper h){
        try(var a=new Arena(h)){
            a.caster.setPos(a.center);a.launch("orbit","ball_lightning");a.carrier.numSensitive=1;
            var block=BlockPos.containing(((EntityOrbitProjectile)a.carrier).getAngledPosition(8));
            h.getLevel().setBlockAndUpdate(block,Blocks.STONE.defaultBlockState());
            try{for(int i=0;i<12 && a.session.active();i++)a.tick();h.assertTrue(a.ball.isRemoved(),"Entity retention does not override sensitive Orbit wall contact");}
            finally{h.getLevel().setBlockAndUpdate(block,Blocks.AIR.defaultBlockState());}
        }
        try(var a=new Arena(h)){
            a.launch("orbit","ball_lightning");a.caster.discard();a.tick();h.assertTrue(a.ball.isRemoved() && a.carrier.isRemoved(),"Owner removal still closes both entities");h.succeed();
        }
    }
    public static void ordinaryBallAndNonpersistentOrbitRemainNative(GameTestHelper h){
        try(var a=new Arena(h)){
            a.launch("ordinary","ball_lightning");h.assertTrue(a.carrier.isRemoved() && !((NativeCastCarrier)a.ball).ironsNouveau$arsDriven(),"Ordinary projectile hands off to the native entity");
            a.ball.tickCount=101;a.ball.tick();h.assertTrue(a.ball.isRemoved(),"Native ordinary ball retains its own expiry");
        }
        try(var a=new Arena(h)){
            a.caster.setPos(a.center);a.launch("orbit","firebolt");a.target(((EntityOrbitProjectile)a.carrier).getAngledPosition(15).add(0,-.5,0));a.collide();
            h.assertTrue(a.ball.isRemoved(),"Non-opted-in Orbit payload keeps existing contact consumption");h.succeed();
        }
    }
    public static void ordinaryBallAndNonpersistentPayloadRemainNative(GameTestHelper h){
        try(var a=new Arena(h)){
            a.launch("ordinary","ball_lightning");h.assertTrue(a.carrier.isRemoved() && !((NativeCastCarrier)a.ball).ironsNouveau$arsDriven(),"Ordinary projectile hands off to the native entity");
            a.ball.tickCount=101;a.ball.tick();h.assertTrue(a.ball.isRemoved(),"Native ordinary ball retains its own expiry");
        }
        try(var a=new Arena(h)){
            a.launch("splash","firebolt");a.target(a.center.add(1,0,0));a.collide();
            h.assertTrue(a.ball.isRemoved(),"Non-opted-in payload keeps existing contact consumption");h.succeed();
        }
    }
}
