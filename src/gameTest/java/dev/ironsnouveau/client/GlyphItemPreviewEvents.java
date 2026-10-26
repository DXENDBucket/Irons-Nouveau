package dev.ironsnouveau.client;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
@EventBusSubscriber(modid="irons_nouveau",value=Dist.CLIENT)
public final class GlyphItemPreviewEvents {
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) { GlyphItemPreview.tryOpen(); }
}
