package dev.ironsnouveau.platform;
public final class MobSpellAttributes {
    public static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        dev.arsconflux.api.glyph.GlyphConditions.declare(dev.ironsnouveau.platform.Locations.id("irons_nouveau:glyph_oakskin"),
                (actor,target)->!target.hasEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.OAKSKIN.get()));
        dev.arsconflux.api.glyph.GlyphConditions.declare(dev.ironsnouveau.platform.Locations.id("irons_nouveau:glyph_charge"),
                (actor,target)->!target.hasEffect(io.redspace.ironsspellbooks.registries.MobEffectRegistry.CHARGED.get()));
        bus.addListener((net.minecraftforge.event.entity.EntityAttributeModificationEvent event)->{
            for(var type:event.getTypes())if(type!=net.minecraft.world.entity.EntityType.PLAYER)
                for(var key:java.util.List.of(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MANA_REGEN,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.SPELL_POWER,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.CAST_TIME_REDUCTION,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.COOLDOWN_REDUCTION,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.FIRE_SPELL_POWER,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ICE_SPELL_POWER,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.LIGHTNING_SPELL_POWER,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.HOLY_SPELL_POWER,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ENDER_SPELL_POWER,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.BLOOD_SPELL_POWER,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.EVOCATION_SPELL_POWER,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.NATURE_SPELL_POWER,
                io.redspace.ironsspellbooks.api.registry.AttributeRegistry.ELDRITCH_SPELL_POWER))
                    if(!event.has(type,key.get()))event.add(type,key.get());
        });
    }
    public static long manaCapacity(net.minecraft.world.entity.LivingEntity actor) {return (long)Math.max(0,actor.getAttributeValue(io.redspace.ironsspellbooks.api.registry.AttributeRegistry.MAX_MANA.get()));}
    private MobSpellAttributes() {}
}
