package dev.ironsnouveau.client;

import dev.ironsnouveau.GlyphCatalog;
import net.minecraft.client.*;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.texture.MissingTextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.*;
import java.util.stream.Stream;

/** One-shot baked-model audit and held/ground screenshot; not included in releases. */
public final class GlyphItemPreview extends Screen {
    private static boolean opened;private int frames;
    private final java.util.List<ItemStack> samples=new java.util.ArrayList<>();
    private GlyphItemPreview() {
        super(Component.literal("Glyph item models"));var mc=Minecraft.getInstance();int count=0;
        for(var part:Stream.concat(GlyphCatalog.glyphs().stream(),GlyphCatalog.forms().stream()).toList()) {
            var stack=part.getGlyph().getDefaultInstance();var model=mc.getItemRenderer().getModel(stack,null,null,0);
            if(model==mc.getModelManager().getMissingModel())throw new AssertionError("Missing baked model: "+part.getRegistryName());
            var quads=model.getQuads(null,null,RandomSource.create(1));if(quads.isEmpty())throw new AssertionError("Empty baked glyph: "+part.getRegistryName());
            for(var quad:quads)if(quad.getSprite().contents().name().equals(MissingTextureAtlasSprite.getLocation()))throw new AssertionError("Unstitched glyph sprite: "+part.getRegistryName());
            count++;
            if(java.util.Set.of("glyph_heal","glyph_fireball","glyph_native/native_fixture/channel").contains(part.getRegistryName().getPath()))samples.add(stack);
        }
        org.slf4j.LoggerFactory.getLogger("Iron glyph item preview").info("PASS: {} baked glyph item models have non-missing atlas sprites",count);
    }
    public static void tryOpen() {
        var mc=Minecraft.getInstance();
        if(Boolean.getBoolean("ironsnouveau.glyph_item_preview")&&!opened&&mc.screen!=null&&mc.getOverlay()==null){opened=true;mc.setScreen(new GlyphItemPreview());}
    }
    @Override public void render(GuiGraphics gui,int mouseX,int mouseY,float partial) {
        gui.fill(0,0,width,height,0xFF302F2B);
        var contexts=new ItemDisplayContext[]{ItemDisplayContext.GUI,ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,ItemDisplayContext.GROUND};
        var labels=new String[]{"GUI","1st person","3rd person","Ground"};
        for(int col=0;col<contexts.length;col++)gui.drawString(font,labels[col],8+col*width/4,8,0xFFFFFFFF);
        for(int row=0;row<samples.size();row++)for(int col=0;col<contexts.length;col++) {
            var stack=samples.get(row);int x=25+col*width/4,y=42+row*65;
            gui.pose().pushPose();
            if(col==0){gui.pose().translate(x,y,100);gui.pose().scale(3,3,3);gui.renderItem(stack,0,0);}
            else {
                gui.pose().translate(x+24,y+24,120);gui.pose().scale(48,-48,48);
                var renderer=minecraft.getItemRenderer();var model=renderer.getModel(stack,null,null,0);
                renderer.render(stack,contexts[col],false,gui.pose(),minecraft.renderBuffers().bufferSource(),0xF000F0,net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY,model);
            }
            gui.pose().popPose();
        }
        gui.flush();
        if(++frames==20)Screenshot.grab(minecraft.gameDirectory,"glyph-item-models.png",minecraft.getMainRenderTarget(),message->{});
        if(frames==45)minecraft.stop();
    }
}
