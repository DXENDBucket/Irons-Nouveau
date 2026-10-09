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
import net.minecraft.world.entity.EntityType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Iron installs its charged overlay on players and its own casters, but glyphs can charge any mob. */
@EventBusSubscriber(modid = IronsNouveau.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class ChargedMobLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {
    private ChargedMobLayer(RenderLayerParent<T, M> parent) { super(parent); }
    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void layers(EntityRenderersEvent.AddLayers event) {
        for (var type : event.getEntityTypes()) {
            if (type == EntityType.PLAYER) continue;
            // Iron's own casters use GeoEntityRenderer and already carry their native layer.
            if (event.getRenderer(type) instanceof LivingEntityRenderer renderer)
                renderer.addLayer(new ChargedMobLayer(renderer));
        }
    }
    @Override public void render(PoseStack pose, MultiBufferSource buffers, int light, T entity,
            float limbSwing, float limbAmount, float partialTick, float age, float yaw, float pitch) {
        if (entity.isInvisible() || !entity.hasEffect(MobEffectRegistry.CHARGED)) return;
        float time = entity.tickCount + partialTick;
        var buffer = buffers.getBuffer(RenderType.energySwirl(EnergySwirlLayer.CHARGE_TEXTURE, time * .02f % 1, time * .01f % 1));
        pose.pushPose();
        pose.scale(1.01f, 1.01f, 1.01f);
        getParentModel().renderToBuffer(pose, buffer, light, OverlayTexture.NO_OVERLAY, 0xFFCCCCCC);
        pose.popPose();
    }
}
