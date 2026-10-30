package dev.ironsnouveau.casting;

import dev.ironsnouveau.bridge.Resolution;
import dev.ironsnouveau.config.SpellLevelConfig;
import dev.arsconflux.api.resource.ResourceCharge;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.spells.*;
import io.redspace.ironsspellbooks.api.events.SpellOnCastEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Native player casting state owns all callbacks, synchronization and channel timing.
 * Only these opt-in casts replace native payment with their original Conflux account. */
public final class NativeSelfCasts {
    private static final Map<ServerPlayer,Lease> ACTIVE=new IdentityHashMap<>();
    private static final Map<ServerPlayer,Set<String>> RECASTS=new IdentityHashMap<>();
    private static final ThreadLocal<Lease> STARTING=new ThreadLocal<>();
    private static final ThreadLocal<Lease> FIRING=new ThreadLocal<>();
    private NativeSelfCasts() {}
    public static final class Lease {
        public final Resolution resolution;
        public final ServerPlayer player;
        public final dev.arsconflux.api.school.SchoolWeights schools=dev.arsconflux.api.school.SchoolDamage.current();
        public boolean failed;
        Lease(Resolution resolution,ServerPlayer player) { this.resolution=resolution;this.player=player; }
    }
    public static Lease lease(AbstractSpell spell,LivingEntity entity) {
        var firing=FIRING.get();
        if(firing!=null&&firing.player==entity&&firing.resolution.spell()==spell)return firing;
        var starting=STARTING.get();
        if(starting!=null&&starting.player==entity&&starting.resolution.spell()==spell)return starting;
        var lease=ACTIVE.get(entity);
        return lease!=null&&lease.resolution.spell()==spell?lease:null;
    }
    public static boolean starting(AbstractSpell spell,LivingEntity entity) {
        var lease=STARTING.get();return lease!=null&&lease.player==entity&&lease.resolution.spell()==spell;
    }
    public static boolean isStarting(AbstractSpell spell) { var lease=STARTING.get();return lease!=null&&lease.resolution.spell()==spell; }
    public static boolean ownsEvent(SpellOnCastEvent event) {
        var frame=FIRING.get();if(frame==null)frame=STARTING.get();
        return frame!=null&&frame.player==event.getEntity()&&frame.resolution.spell().getSpellId().equals(event.getSpellId());
    }
    public static boolean start(Resolution resolution) {
        if(!(resolution.caster() instanceof ServerPlayer player)||dev.ironsnouveau.platform.Casters.isFake(player))return false;
        var data=MagicData.getPlayerMagicData(player);
        if(data.isCasting()||ACTIVE.containsKey(player))return false;
        if(!data.getPlayerRecasts().hasRecastForSpell(resolution.spell().getSpellId())
                &&TriggerMana.of(resolution.context()).charge(resolution.definition().spellId(),resolution.level()).affordableCount(1)==0)return false;
        var lease=new Lease(resolution,player);var previous=STARTING.get();STARTING.set(lease);ACTIVE.put(player,lease);
        boolean started=false;
        try {
            // NONE prevents a second native mana withdrawal. Native castSpell still
            // fires its event, client callback, recast accounting and completion.
            var tool=resolution.context().ars()==null?player.getMainHandItem():resolution.context().ars().getCasterTool();
            if(tool.isEmpty())tool=player.getMainHandItem();
            String slot=net.minecraft.world.item.ItemStack.matches(tool,player.getOffhandItem())&&!player.getOffhandItem().isEmpty()?"offhand":"mainhand";
            started=resolution.spell().attemptInitiateCast(tool,resolution.level(),resolution.world(),player,CastSource.NONE,true,slot);
            if(!started)return false;
            boolean channel=resolution.spell().getCastType()==CastType.CONTINUOUS;
            resolution.spell().castSpell(resolution.world(),resolution.level(),player,CastSource.NONE,!channel);
            if(!channel||lease.failed) {
                try { resolution.spell().onServerCastComplete(resolution.world(),resolution.level(),player,data,lease.failed); }
                finally {ACTIVE.remove(player);}
            } else data.handleCastDuration(); // First pulse already fired; don't repeat it on the next tick.
            return !lease.failed;
        } catch(RuntimeException|Error failure) {
            try {
                if(data.isCasting()&&data.getCastingSpellId().equals(resolution.spell().getSpellId()))
                    resolution.spell().onServerCastComplete(resolution.world(),resolution.level(),player,data,true);
            } catch(RuntimeException|Error cleanup) {failure.addSuppressed(cleanup);}
            finally {ACTIVE.remove(player);}
            throw failure;
        } finally {
            if(!started)ACTIVE.remove(player);
            if(previous==null)STARTING.remove();else STARTING.set(previous);
        }
    }
    public static void around(AbstractSpell spell,ServerPlayer player,Runnable action) {
        var lease=lease(spell,player);
        if(lease==null){action.run();return;}
        var previous=FIRING.get();FIRING.set(lease);
        try {
            dev.arsconflux.api.school.SchoolDamage.during(lease.schools,()->dev.arsconflux.api.context.CastContexts.scoped(lease.resolution.context(),()->{action.run();return null;}));
            if(MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(spell.getSpellId()))
                RECASTS.computeIfAbsent(player,p->new HashSet<>()).add(spell.getSpellId());
        }
        finally { if(previous==null)FIRING.remove();else FIRING.set(previous); }
    }
    public static void effect(AbstractSpell spell,LivingEntity caster,SpellOnCastEvent event,Runnable action) {
        var lease=lease(spell,caster);
        if(lease==null){action.run();return;}
        boolean prepaid=MagicData.getPlayerMagicData(caster).getPlayerRecasts().hasRecastForSpell(spell.getSpellId());
        var charge=new ResourceCharge(TriggerMana.of(lease.resolution.context()).account(),prepaid?0:Math.max(0,event.getManaCost()));
        if(!charge.trigger(()->{action.run();return true;}))lease.failed=true;
    }
    public static boolean failed(AbstractSpell spell,LivingEntity caster) {
        var lease=lease(spell,caster);return lease!=null&&lease.failed;
    }
    public static void tick() {
        RECASTS.entrySet().removeIf(entry->{
            var player=entry.getKey();
            entry.getValue().removeIf(id->!MagicData.getPlayerMagicData(player).getPlayerRecasts().hasRecastForSpell(id));
            return player.hasDisconnected()||!player.isAlive()||entry.getValue().isEmpty();
        });
        for(var entry:new ArrayList<>(ACTIVE.entrySet())) {
            var player=entry.getKey();var lease=entry.getValue();var data=MagicData.getPlayerMagicData(player);
            if(!data.isCasting()||!data.getCastingSpellId().equals(lease.resolution.spell().getSpellId())) {ACTIVE.remove(player);continue;}
            if(lease.failed||!player.isAlive()||player.hasDisconnected()||player.level()!=lease.resolution.world()||!lease.resolution.spell().isEnabled()||!SpellLevelConfig.nativeSelfEnabled()) {
                try { io.redspace.ironsspellbooks.api.util.Utils.serverSideCancelCast(player); }
                finally {ACTIVE.remove(player);}
            }
        }
    }
    public static boolean managedCooldown(AbstractSpell spell,ServerPlayer player) {
        return lease(spell,player)!=null||RECASTS.getOrDefault(player,Set.of()).contains(spell.getSpellId());
    }
    public static void stop() { ACTIVE.clear();RECASTS.clear();STARTING.remove();FIRING.remove(); }
}
