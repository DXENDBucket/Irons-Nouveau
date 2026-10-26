package dev.ironsnouveau;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import io.redspace.ironsspellbooks.api.config.DefaultConfig;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.api.registry.*;
import io.redspace.ironsspellbooks.api.spells.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.LivingEntity;

public final class NativeSelfFixture {
    public static void register(IEventBus bus) {
        var spells=DeferredRegister.create(SpellRegistry.SPELL_REGISTRY_KEY,"native_fixture");
        spells.register("instant",()->new Sample("instant",CastType.INSTANT));
        spells.register("long",()->new Sample("long",CastType.LONG));
        spells.register("channel",()->new Sample("channel",CastType.CONTINUOUS));
        spells.register(bus);
    }
    public static final class Sample extends AbstractSpell {
        private final ResourceLocation id;private final CastType type;
        Sample(String name,CastType type) {
            id=dev.ironsnouveau.platform.Locations.id("native_fixture:"+name);this.type=type;
            baseManaCost=7;manaCostPerLevel=0;baseSpellPower=4;spellPowerPerLevel=2;castTime=40;
        }
        public ResourceLocation getSpellResource(){return id;}
        public CastType getCastType(){return type;}
        public DefaultConfig getDefaultConfig(){return new DefaultConfig().setMaxLevel(5).setMinRarity(SpellRarity.COMMON).setCooldownSeconds(1).setSchoolResource(SchoolRegistry.FIRE_RESOURCE).build();}
        public boolean checkPreCastConditions(Level world,int level,LivingEntity caster,MagicData data){return !caster.getPersistentData().getBoolean("fixture_reject");}
        private void count(LivingEntity caster,String key){caster.getPersistentData().putInt(key,caster.getPersistentData().getInt(key)+1);}
        public void onServerPreCast(Level world,int level,LivingEntity caster,MagicData data){super.onServerPreCast(world,level,caster,data);count(caster,"fixture_pre");}
        public void onCast(Level world,int level,LivingEntity caster,CastSource source,MagicData data){count(caster,"fixture_calls");SpellRegistry.FIREBOLT_SPELL.get().onCast(world,level,caster,source,data);}
        public void onServerCastTick(Level world,int level,LivingEntity caster,MagicData data){count(caster,"fixture_ticks");}
        public void onServerCastComplete(Level world,int level,LivingEntity caster,MagicData data,boolean cancelled){count(caster,"fixture_complete");caster.getPersistentData().putBoolean("fixture_cancelled",cancelled);super.onServerCastComplete(world,level,caster,data,cancelled);}
    }
}
