package dev.local.haze.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.local.haze.sim.LocalImageSlice;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import org.joml.Matrix3x2f;

/** Fractional geometry and matching UVs keep the fill inside its original opacity mask. */
final class LocalClippedImage {
    static void draw(GuiGraphics graphics,String path,LocalImageSlice slice,int color){
        if(slice.empty())return;
        var texture=Minecraft.getInstance().getTextureManager().getTexture(Identifier.fromNamespaceAndPath("haze","retail/"+path));
        Matrix3x2f pose=new Matrix3x2f(graphics.pose());
        int left=(int)Math.floor(slice.x0()),top=(int)Math.floor(slice.y0());
        ScreenRectangle bounds=new ScreenRectangle(left,top,(int)Math.ceil(slice.x1())-left,(int)Math.ceil(slice.y1())-top).transformMaxBounds(pose);
        ScreenRectangle scissor=graphics.scissorStack.peek();
        if(scissor!=null)bounds=scissor.intersection(bounds);
        if(bounds!=null)graphics.guiRenderState.submitGuiElement(new Quad(TextureSetup.singleTexture(texture.getTextureView(),texture.getSampler()),pose,slice,color,scissor,bounds));
    }
    private record Quad(TextureSetup textureSetup,Matrix3x2f pose,LocalImageSlice slice,int color,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public RenderPipeline pipeline(){return RenderPipelines.GUI_TEXTURED;}
        @Override public void buildVertices(VertexConsumer out){
            out.addVertexWith2DPose(pose,slice.x0(),slice.y0()).setUv(slice.u0(),slice.v0()).setColor(color);
            out.addVertexWith2DPose(pose,slice.x0(),slice.y1()).setUv(slice.u0(),slice.v1()).setColor(color);
            out.addVertexWith2DPose(pose,slice.x1(),slice.y1()).setUv(slice.u1(),slice.v1()).setColor(color);
            out.addVertexWith2DPose(pose,slice.x1(),slice.y0()).setUv(slice.u1(),slice.v0()).setColor(color);
        }
    }
}
