package dev.ironsnouveau.mixin;
import dev.ironsnouveau.compat.NativeGlyphResources;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelBakery.class)
public abstract class NativeGlyphModelMixin {
    @Inject(method="loadBlockModel",at=@At("HEAD"),cancellable=true)
    private void ironsNouveau$nativeModel(ResourceLocation id,CallbackInfoReturnable<BlockModel> cir) {
        var resources=net.minecraft.client.Minecraft.getInstance().getResourceManager();
        var model=NativeGlyphResources.model(id,texture->resources.getResource(texture).isPresent());
        if(model!=null){var parsed=BlockModel.fromString(model);parsed.name=id.toString();cir.setReturnValue(parsed);}
    }
}
