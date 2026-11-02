package dev.ironsnouveau.ability;
import dev.arsconflux.api.execution.ExecutionSession;
import io.redspace.ironsspellbooks.entity.spells.fiery_dagger.FieryDaggerEntity;
public interface DaggerState {
    ExecutionSession<FieryDaggerEntity> ironsNouveau$abilitySession();
    void ironsNouveau$abilitySession(ExecutionSession<FieryDaggerEntity> session);
    com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell ironsNouveau$trajectory();
    void ironsNouveau$trajectory(com.hollingsworth.arsnouveau.common.entity.EntityProjectileSpell trajectory);
}
