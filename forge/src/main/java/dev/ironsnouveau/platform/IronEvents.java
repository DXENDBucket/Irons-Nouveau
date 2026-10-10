package dev.ironsnouveau.platform;

import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.events.SpellSummonEvent;

public final class IronEvents {
    private IronEvents() {}
    public static void heal(SpellHealEvent event) { net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event); }
    public static boolean counterspell(CounterSpellEvent event) {
        return net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event);
    }
    public static void summon(SpellSummonEvent<?> event) { net.minecraftforge.common.MinecraftForge.EVENT_BUS.post(event); }
}
