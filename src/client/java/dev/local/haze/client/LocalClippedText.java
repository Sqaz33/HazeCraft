package dev.local.haze.client;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.local.haze.sim.LocalTextClip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.gui.render.state.GuiElementRenderState;
import net.minecraft.network.chat.Component;
import org.joml.Matrix3x2f;
import org.joml.Matrix3x2fc;
import org.joml.Matrix4f;
import java.util.*;

/** Uses the existing Oracle glyphs; only the partial glyph is cropped before transformation. */
final class LocalClippedText {
    static void draw(GuiGraphics graphics,Font font,Component text,int x,int y,int color,float cutoff){
        Matrix3x2f pose=new Matrix3x2f(graphics.pose());ScreenRectangle scissor=graphics.scissorStack.peek();
        font.prepareText(text.getVisualOrderText(),x,y,color,false,false,0).visit(new Font.GlyphVisitor(){
            @Override public void acceptGlyph(TextRenderable.Styled glyph){submit(glyph);}
            @Override public void acceptEffect(TextRenderable glyph){submit(glyph);}
            private void submit(TextRenderable glyph){
                if(glyph.left()>=cutoff)return;
                int left=(int)Math.floor(glyph.left()),top=(int)Math.floor(glyph.top());
                int right=(int)Math.ceil(Math.min(glyph.right(),cutoff)),bottom=(int)Math.ceil(glyph.bottom());
                if(right<=left||bottom<=top)return;
                ScreenRectangle bounds=new ScreenRectangle(left,top,right-left,bottom-top).transformMaxBounds(pose);
                if(scissor!=null)bounds=scissor.intersection(bounds);
                if(bounds!=null)graphics.guiRenderState.submitGuiElement(new Glyph(pose,glyph,cutoff,scissor,bounds));
            }
        });
    }
    private record Glyph(Matrix3x2fc pose,TextRenderable renderable,float cutoff,ScreenRectangle scissorArea,ScreenRectangle bounds) implements GuiElementRenderState {
        @Override public RenderPipeline pipeline(){return renderable.guiPipeline();}
        @Override public TextureSetup textureSetup(){return TextureSetup.singleTextureWithLightmap(renderable.textureView(),RenderSystem.getSamplerCache().getClampToEdge(FilterMode.NEAREST));}
        @Override public void buildVertices(VertexConsumer output){
            Matrix4f transform=new Matrix4f().mul(pose);
            if(renderable.right()<=cutoff){renderable.render(transform,output,15728880,true);return;}
            Collector collector=new Collector(output,transform,cutoff);
            renderable.render(new Matrix4f(),collector,15728880,true);collector.flush();
        }
    }
    private static final class Collector implements VertexConsumer {
        private final VertexConsumer output;
        private final Matrix4f transform;
        private final float cutoff;
        private final float[][] vertices=new float[4][5];
        private int count,color,light;
        Collector(VertexConsumer output,Matrix4f transform,float cutoff){this.output=output;this.transform=transform;this.cutoff=cutoff;}
        @Override public VertexConsumer addVertex(float x,float y,float z){
            if(count==4)flush();float[] vertex=vertices[count++];vertex[0]=x;vertex[1]=y;vertex[2]=z;return this;
        }
        @Override public VertexConsumer setColor(int r,int g,int b,int a){color=a<<24|r<<16|g<<8|b;return this;}
        @Override public VertexConsumer setColor(int value){color=value;return this;}
        @Override public VertexConsumer setUv(float u,float v){vertices[count-1][3]=u;vertices[count-1][4]=v;return this;}
        @Override public VertexConsumer setUv1(int u,int v){return this;}
        @Override public VertexConsumer setUv2(int u,int v){light=u|v<<16;return this;}
        @Override public VertexConsumer setNormal(float x,float y,float z){return this;}
        @Override public VertexConsumer setLineWidth(float value){return this;}
        void flush(){
            if(count==0)return;if(count!=4)throw new IllegalStateException("Text glyph is not a quad");
            List<LocalTextClip.Vertex> quad=new ArrayList<>(4);
            for(float[] v:vertices)quad.add(new LocalTextClip.Vertex(v[0],v[1],v[2],v[3],v[4]));
            var polygon=LocalTextClip.right(quad,cutoff);
            // Font pipelines consume quads; a duplicated final corner represents each clipped triangle.
            for(int i=1;i+1<polygon.size();i++){emit(polygon.getFirst());emit(polygon.get(i));emit(polygon.get(i+1));emit(polygon.get(i+1));}
            count=0;
        }
        private void emit(LocalTextClip.Vertex v){output.addVertex(transform,v.x(),v.y(),v.z()).setColor(color).setUv(v.u(),v.v()).setLight(light);}
    }
}
