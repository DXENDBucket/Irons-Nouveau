package dev.ironsnouveau.client;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.ironsnouveau.IronsNouveau;
import io.redspace.ironsspellbooks.registries.MobEffectRegistry;
import io.redspace.ironsspellbooks.render.EnergySwirlLayer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod.EventBusSubscriber;
import net.minecraftforge.client.event.RenderLivingEvent;

/** Iron installs its charged overlay on players and its own casters, but glyphs can charge any mob. */
@EventBusSubscriber(modid = IronsNouveau.MOD_ID, value = Dist.CLIENT)
public final class ChargedMobLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private ChargedMobLayer(RenderLayerParent<T, M> parent) { super(parent); }
    private static final java.util.Set<LivingEntityRenderer<?, ?>> INSTALLED = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void layers(RenderLivingEvent.Pre<?, ?> event) {
        if (event.getEntity() instanceof net.minecraft.world.entity.player.Player) return;
        LivingEntityRenderer renderer = event.getRenderer();
        if (INSTALLED.add(renderer)) renderer.addLayer(new ChargedMobLayer(renderer));
    }
    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity,
            float limbSwing, float limbAmount, float partialTick, float age, float yaw, float pitch) {
        if (entity.isInvisible() || !dev.ironsnouveau.platform.Effects.has(entity, MobEffectRegistry.CHARGED)) return;
        float time = entity.tickCount + partialTick;
        var buffer = buffers.getBuffer(RenderType.energySwirl(EnergySwirlLayer.CHARGE_TEXTURE, time * .02f % 1, time * .01f % 1));
        pose.pushPose();
        pose.scale(1.01f, 1.01f, 1.01f);
        getParentModel().renderToBuffer(pose, buffer, light, OverlayTexture.NO_OVERLAY, .8f, .8f, .8f, 1f);
        pose.popPose();
    }
}
