package dev.ironsnouveau.platform;

import io.redspace.ironsspellbooks.api.events.SpellHealEvent;
import io.redspace.ironsspellbooks.api.events.CounterSpellEvent;
import io.redspace.ironsspellbooks.api.events.SpellSummonEvent;

public final class IronEvents {
    private IronEvents() {}
    public static void heal(SpellHealEvent event) { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event); }
    public static boolean counterspell(CounterSpellEvent event) {
        return net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event).isCanceled();
    }
    public static void summon(SpellSummonEvent<?> event) { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(event); }
}
