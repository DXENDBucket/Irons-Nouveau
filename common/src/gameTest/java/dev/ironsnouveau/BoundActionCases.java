package dev.ironsnouveau;

import dev.arsconflux.api.context.CastContext;
import dev.arsconflux.api.execution.*;
import dev.ironsnouveau.casting.BoundSpellSupport;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.*;
import java.util.*;

public final class BoundActionCases {
    public static void busyClaims(GameTestHelper h){
        var actor=h.spawn(EntityType.COW,2,3,2);actor.setNoAi(true);actor.setNoGravity(true);
        var sessions=new ArrayList<ExecutionSession<Integer>>();
        try {
            var action=session(h,actor,List.of(ExecutionClaim.action(actor)));sessions.add(action);ExecutionSessions.start(action);
            h.assertTrue(ExecutionSessions.isCasting(actor) && !BoundSpellSupport.busy(actor),"A physical action does not block bound-tool release before replacement can run");
            var channel=session(h,actor,List.of(ExecutionClaim.channel(actor)));sessions.add(channel);ExecutionSessions.start(channel);
            h.assertTrue(BoundSpellSupport.busy(actor),"A continuing channel retains the existing bound-tool busy restriction");
            channel.finish(ExecutionSession.EndReason.COMPLETED);
            var activity=session(h,actor,List.of(new ExecutionClaim(actor,ExecutionClaim.ACTIVITY,false)));sessions.add(activity);ExecutionSessions.start(activity);
            h.assertTrue(BoundSpellSupport.busy(actor),"Legacy activity is not accidentally ignored as a replaceable body action");
            activity.finish(ExecutionSession.EndReason.COMPLETED);
            h.assertTrue(!BoundSpellSupport.busy(actor) && action.active(),"Ending unrelated activity does not end the body action");h.succeed();
        }finally{for(var s:sessions)s.finish(ExecutionSession.EndReason.SERVER_STOPPED);actor.discard();ExecutionSessions.tick();}
    }
    private static ExecutionSession<Integer> session(GameTestHelper h,LivingEntity actor,List<ExecutionClaim> claims){
        return new ExecutionSession<>(h.getLevel(),CastContext.detached(actor,null,null),0,40,new ExecutionDriver<Integer>(){
            public List<ExecutionClaim> claims(ExecutionSession<Integer> s){return claims;}
            public boolean start(ExecutionSession<Integer> s){return true;}
            public boolean tick(ExecutionSession<Integer> s){return false;}
            public void close(ExecutionSession<Integer> s,ExecutionSession.EndReason reason){}
        },hit->{},()->true,ExecutionScope.DIRECT);
    }
}
