package dev.ironsnouveau.mixin;
import com.hollingsworth.arsnouveau.common.capability.IPlayerCap;
import com.hollingsworth.arsnouveau.setup.registry.CapabilityRegistry;
import dev.ironsnouveau.progression.KnowledgeOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.util.LazyOptional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(value = CapabilityRegistry.class, remap = false)
public abstract class KnowledgeOwnerMixin {
    @Inject(method = "getPlayerDataCap", at = @At("RETURN"))
    private static void ironsNouveau$owner(LivingEntity entity, CallbackInfoReturnable<LazyOptional<IPlayerCap>> ci) {
        ci.getReturnValue().ifPresent(cap -> { if (cap instanceof KnowledgeOwner owned) owned.ironsNouveau$owner(entity); });
    }
}
