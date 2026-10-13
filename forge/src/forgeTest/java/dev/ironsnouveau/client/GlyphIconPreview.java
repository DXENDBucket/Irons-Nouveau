package dev.ironsnouveau.client;

import com.hollingsworth.arsnouveau.client.gui.utils.RenderUtils;
import com.hollingsworth.arsnouveau.common.items.Glyph;
import dev.arsconflux.config.ConfluxClientConfig;
import dev.arsconflux.internal.presentation.GlyphFrameStyle;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.TickEvent;

/** Opt-in visual regression harness; excluded from release and sources JARs. */
@Mod.EventBusSubscriber(modid="irons_nouveau", value=Dist.CLIENT)
public final class GlyphIconPreview extends Screen {
    private static boolean opened;
    private int frames;
    private GlyphIconPreview() { super(Component.literal("Glyph icon preview")); }
    @SubscribeEvent public static void tick(TickEvent.ClientTickEvent event) {
        if(event.phase!=TickEvent.Phase.END)return;
        var mc=Minecraft.getInstance();
        if(Boolean.getBoolean("ironsnouveau.icon_preview") && !opened && mc.screen!=null && mc.getOverlay()==null) {
            opened=true;mc.setScreen(new GlyphIconPreview());
        }
    }
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void config(String name,Object value) {
        try {
            var field=ConfluxClientConfig.class.getDeclaredField(name);field.setAccessible(true);
            ((ForgeConfigSpec.ConfigValue)field.get(null)).set(value);
        } catch(ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    public void render(GuiGraphics gui,int mouseX,int mouseY,float partial) {
        gui.fill(0,0,width,height,0xFF302F2B);
        gui.drawString(font,"Native Ars | Iron effect | Iron augment | Iron effect | Effect faded",8,8,0xFFFFFFFF);
        String[] ids={"ars_nouveau:glyph_light", "irons_nouveau:glyph_heal", "irons_nouveau:glyph_fireball",
                "irons_nouveau:glyph_heal", "irons_nouveau:glyph_heal"};
        String[] names={"Ars types", "Uniform", "Custom", "Original sources"};
        for(int row=0;row<4;row++) {
            config("ENABLED",row<3);config("PROVIDER_ICONS",row<3);
            config("MODE",row==1?GlyphFrameStyle.Mode.UNIFORM:row==2?GlyphFrameStyle.Mode.CUSTOM:GlyphFrameStyle.Mode.ARS_TYPE);
            config("EFFECT","#F08060");config("AUGMENT","#70CC70");
            int y=28+row*64;
            gui.drawString(font,names[row],8,y,0xFFDDCCAA);
            for(int col=0;col<ids.length;col++) {
                var item=BuiltInRegistries.ITEM.get(new ResourceLocation(ids[col]));
                if(!(item instanceof Glyph glyph))throw new IllegalStateException("Missing preview glyph "+ids[col]);
                gui.pose().pushPose();gui.pose().translate(8+col*64,y+12,0);gui.pose().scale(3,3,3);
                if(col==4)RenderUtils.drawSpellPart(glyph.spellPart,gui,0,0,16,true,0);
                else gui.renderItem(new ItemStack(item),0,0);
                gui.pose().popPose();
            }
        }
        gui.flush();
        if(++frames==30)Screenshot.grab(minecraft.gameDirectory,"glyph-icons.png",minecraft.getMainRenderTarget(),message->{});
        if(frames==90)minecraft.stop();
    }
}
