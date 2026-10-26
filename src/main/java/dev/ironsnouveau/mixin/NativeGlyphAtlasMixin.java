package dev.ironsnouveau.mixin;

import dev.ironsnouveau.compat.NativeGlyphResources;
import net.minecraft.client.renderer.texture.atlas.*;
import net.minecraft.client.renderer.texture.atlas.sources.SingleFile;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.*;

/** NeoForge atlas hook: GUI PNGs aren't included by vanilla item directory sources. */
@Mixin(SpriteSourceList.class)
public abstract class NativeGlyphAtlasMixin {
    @Shadow @Final private List<SpriteSource> sources;
    @Inject(method="load",at=@At("RETURN"))
    private static void ironsNouveau$icons(ResourceManager resources,ResourceLocation atlas,CallbackInfoReturnable<SpriteSourceList> cir) {
        if(!atlas.toString().equals("minecraft:blocks"))return;
        var sources=((NativeGlyphAtlasMixin)(Object)cir.getReturnValue()).sources;
        for(var texture:NativeGlyphResources.textures())if(resources.getResource(texture).isPresent())
            sources.add(new SingleFile(NativeGlyphResources.sprite(texture),Optional.empty()));
    }
}
