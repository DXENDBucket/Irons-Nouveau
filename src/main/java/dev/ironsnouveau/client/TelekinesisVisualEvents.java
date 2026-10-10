package dev.ironsnouveau.client;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
@EventBusSubscriber(modid = "irons_nouveau", value = Dist.CLIENT)
public final class TelekinesisVisualEvents {
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) { TelekinesisVisuals.tick(); }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            TelekinesisVisuals.render(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(),
                    event.getCamera().getPosition(), event.getPartialTick().getGameTimeDeltaPartialTick(false));
    }
}
