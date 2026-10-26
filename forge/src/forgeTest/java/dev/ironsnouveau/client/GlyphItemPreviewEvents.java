package dev.ironsnouveau.client;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.TickEvent;
@Mod.EventBusSubscriber(modid="irons_nouveau",value=Dist.CLIENT)
public final class GlyphItemPreviewEvents {
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase==TickEvent.Phase.END) GlyphItemPreview.tryOpen();
    }
}
