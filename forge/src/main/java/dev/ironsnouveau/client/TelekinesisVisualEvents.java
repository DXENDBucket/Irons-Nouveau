package dev.ironsnouveau.client;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.client.event.RenderLevelStageEvent;
@Mod.EventBusSubscriber(modid = "irons_nouveau", value = Dist.CLIENT)
public final class TelekinesisVisualEvents {
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) TelekinesisVisuals.tick();
    }
    @SubscribeEvent public static void render(RenderLevelStageEvent event) {
        if (event.getStage() == RenderLevelStageEvent.Stage.AFTER_PARTICLES)
            TelekinesisVisuals.render(event.getPoseStack(), Minecraft.getInstance().renderBuffers().bufferSource(),
                    event.getCamera().getPosition(), event.getPartialTick());
    }
}
