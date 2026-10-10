package dev.ironsnouveau.casting;

import com.hollingsworth.arsnouveau.api.spell.SpellContext;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.IWrappedCaster;
import com.hollingsworth.arsnouveau.api.spell.wrapped_caster.LivingCaster;
import io.redspace.ironsspellbooks.api.registry.SpellRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import dev.arsconflux.api.resource.*;
import dev.arsconflux.api.context.CastContext;
import java.util.function.BooleanSupplier;

/** Synchronous reservation prevents nested effects spending the same mana. Charge only committed triggers. */
public record TriggerMana(LivingEntity caster, IWrappedCaster source, ResourceAccount paymentAccount) {
    public TriggerMana(LivingEntity caster, IWrappedCaster source) { this(caster, source, null); }
    private static final ThreadLocal<TriggerMana> CURRENT = new ThreadLocal<>();
    public static TriggerMana current() { return CURRENT.get(); }
    public static TriggerMana of(SpellContext context, LivingEntity caster) {
        return context == null ? new TriggerMana(caster, LivingCaster.from(caster)) : of(CastContext.of(context, null, null));
    }
    public static TriggerMana of(CastContext context) {
        return new TriggerMana(context.caster(), context.ars() == null ? LivingCaster.from(context.caster()) : context.ars().getCaster(), context.account());
    }
    /** Ars upfront cost belongs to this account only when using Ars mana. */
    public boolean canBegin(ResourceLocation spell, int level, int arsUpfrontCost) {
        return ResourceTransactions.canSpend(account(), (long)cost(spell, level) + (account().reservationKey().equals(new ArsManaAccount(source).reservationKey()) ? Math.max(0, arsUpfrontCost) : 0));
    }
    private int cost(ResourceLocation spell, int level) { return Math.max(0, SpellRegistry.getSpell(spell).getManaCost(level)); }
    public ResourceCharge charge(ResourceLocation spell, int level) { return new ResourceCharge(account(), cost(spell, level)); }
    public int affordableCount(ResourceLocation spell, int level, int requested) { return charge(spell, level).affordableCount(requested); }
    public boolean trigger(ResourceLocation spell, int level, BooleanSupplier action) {
        if (!caster.isAlive() || caster.level().isClientSide) return false;
        return charge(spell, level).trigger(() -> run(action));
    }
    public ResourceAccount account() {
        if (paymentAccount != null) return paymentAccount;
        if (!usesIronMana()) return new ArsManaAccount(source,
                source instanceof LivingCaster living ? living.livingEntity : source,
                caster instanceof Player player && player.isCreative());
        return new ResourceAccount() {
            public Object reservationKey() { return ResourcePoolKey.of("irons_spellbooks:mana", caster); }
            public boolean free() { return caster instanceof Player player && player.isCreative(); }
            public boolean canSpend(int amount) { return enoughMana(amount); }
            public void spend(int amount) { expendMana(amount); }
        };
    }
    private boolean usesIronMana() {
        return dev.ironsnouveau.config.SpellLevelConfig.useIronMana()
                && caster instanceof net.minecraft.server.level.ServerPlayer
                && !(caster instanceof net.neoforged.neoforge.common.util.FakePlayer);
    }
    private boolean enoughMana(int cost) {
        if (cost == 0) return true;
        return io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(caster).getMana() >= cost;
    }
    private void expendMana(int cost) {
        var data = io.redspace.ironsspellbooks.api.magic.MagicData.getPlayerMagicData(caster);
        data.setMana(Math.max(0, data.getMana() - cost));
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer((net.minecraft.server.level.ServerPlayer)caster,
                new io.redspace.ironsspellbooks.network.SyncManaPacket(data));
    }
    private boolean run(BooleanSupplier action) {
        var previous = CURRENT.get(); CURRENT.set(this);
        try { return action.getAsBoolean(); }
        finally { if (previous == null) CURRENT.remove(); else CURRENT.set(previous); }
    }
}
