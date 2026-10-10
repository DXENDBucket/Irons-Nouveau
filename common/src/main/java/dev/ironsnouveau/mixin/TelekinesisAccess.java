package dev.ironsnouveau.mixin;
import io.redspace.ironsspellbooks.api.magic.MagicData;
import io.redspace.ironsspellbooks.spells.eldritch.TelekinesisSpell;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(value = TelekinesisSpell.class, remap = false)
public interface TelekinesisAccess {
    @Invoker("handleTelekinesis") void ironsNouveau$handle(ServerLevel world, LivingEntity caster, MagicData data, float strength);
}
