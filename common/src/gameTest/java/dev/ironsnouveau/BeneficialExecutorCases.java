package dev.ironsnouveau;
import com.hollingsworth.arsnouveau.api.spell.*;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import com.hollingsworth.arsnouveau.common.spell.method.MethodTouch;
import dev.arsconflux.api.context.*;
import dev.arsconflux.api.execution.ExecutionSessions;
import dev.arsconflux.api.resource.ResourceAccount;
import dev.ironsnouveau.glyph.BridgeGlyph;
import io.redspace.ironsspellbooks.entity.spells.AbstractConeProjectile;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import java.util.concurrent.atomic.AtomicInteger;

public final class BeneficialExecutorCases {
    private static BridgeGlyph glyph(String id) {
        return IronsNouveau.glyphs().stream().filter(g->g.definition().spellId().getPath().equals(id)).findFirst().orElseThrow();
    }
    private static LivingEntity cow(GameTestHelper h,int x,int z) {
        var cow=h.spawn(EntityType.COW,x,2,z);cow.setNoAi(true);cow.setNoGravity(true);cow.setYRot(0);return cow;
    }
    private static void cast(GameTestHelper h,LivingEntity caster,String id,LivingEntity host,AtomicInteger balance) {
        var account=new ResourceAccount() {
            public Object reservationKey(){return balance;}
            public boolean canSpend(int cost){return balance.get()>=cost;}
            public void spend(int cost){balance.addAndGet(-cost);}
        };
        var g=glyph(id);
        var ars=new SpellContext(h.getLevel(),new Spell(MethodTouch.INSTANCE,g),caster,new LivingCaster(caster),ItemStack.EMPTY);
        CastContexts.bind(ars,CastContext.of(ars,null,account));
        g.onResolve(new EntityHitResult(host),h.getLevel(),caster,new SpellStats.Builder().build(),ars,new SpellResolver(ars));
    }
    public static void ally(GameTestHelper h,LivingEntity caster,LivingEntity host) {
        var board=h.getLevel().getScoreboard();var team=board.getPlayersTeam(caster.getScoreboardName());
        if(team==null) team=board.addPlayerTeam("iron_"+java.util.UUID.randomUUID().toString().substring(0,8));
        board.addPlayerToTeam(caster.getScoreboardName(),team);board.addPlayerToTeam(host.getScoreboardName(),team);
    }
    public static void hostAuthorizationAndCancellation(GameTestHelper h) {
        var caster=cow(h,1,1);var host=cow(h,5,5);var balance=new AtomicInteger(100000);
        var start=host.position();
        for(var id:java.util.List.of("fire_breath","poison_breath","dragon_breath","cone_of_cold","electrocute","ray_of_siphoning","ray_of_frost",
                "burning_dash","volt_strike","shadow_slash","ascension")) cast(h,caster,id,host,balance);
        h.assertTrue(balance.get()==100000&&host.position().equals(start)&&host.getDeltaMovement().lengthSqr()==0,
                "Hostile hosts receive no breath, ray, motion, effect or payment");
        h.assertTrue(!ExecutionSessions.isCasting(caster),"Rejected hosts create no sustained execution");
        var board=h.getLevel().getScoreboard();var team=board.addPlayerTeam("iron_"+java.util.UUID.randomUUID().toString().substring(0,8));
        board.addPlayerToTeam(caster.getScoreboardName(),team);board.addPlayerToTeam(host.getScoreboardName(),team);
        cast(h,caster,"fire_breath",host,balance);
        var cones=h.getLevel().getEntitiesOfClass(AbstractConeProjectile.class,host.getBoundingBox().inflate(20));
        h.assertTrue(balance.get()<100000&&cones.size()==1&&ExecutionSessions.isCasting(host)&&!ExecutionSessions.isCasting(caster),"A real ally receives a paid native cone execution");
        board.removePlayerFromTeam(host.getScoreboardName());ExecutionSessions.tick();
        h.assertTrue(cones.get(0).isRemoved()&&!ExecutionSessions.isCasting(host),"Losing ally status ends and removes a running cone");
        board.addPlayerToTeam(host.getScoreboardName(),team);
        var before=balance.get();cast(h,caster,"volt_strike",host,balance);
        h.assertTrue(balance.get()<before&&host.getDeltaMovement().lengthSqr()>0,"Native motion remains available to allies");
        before=balance.get();cast(h,caster,"ray_of_siphoning",caster,balance);
        h.assertTrue(balance.get()<before&&ExecutionSessions.isCasting(caster),"Self remains a valid continuous ray executor");
        caster.discard();host.discard();ExecutionSessions.tick();h.succeed();
    }
}
