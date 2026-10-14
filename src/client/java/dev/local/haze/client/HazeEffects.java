package dev.local.haze.client;

import dev.local.haze.sim.*;
import com.google.gson.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** First supported Source effect: the original Haze additive tracer rope. */
public final class HazeEffects {
    private static final JsonObject DEFINITIONS=load();
    private static final JsonObject TRAIL=DEFINITIONS.getAsJsonObject("particles/weapon_fx/haze/haze_tracer_trail.vpcf");
    private static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("haze","pipeline/tracer"))
        .withVertexFormat(DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.QUADS)
        .withBlend(BlendFunction.LIGHTNING).withCull(false).withDepthWrite(false).build();
    private static final RenderType TYPE=RenderType.create("haze_retail_tracer",RenderSetup.builder(PIPELINE)
        .withTexture("Sampler0",Identifier.fromNamespaceAndPath("haze","retail/materials/particle/beam_generic_2.png"))
        .useLightmap().useOverlay().createRenderSetup());
    private static JsonObject load(){try(var in=HazeEffects.class.getResourceAsStream("/haze/retail/particles.json")){return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new UncheckedIOException(e);}}
    private static double lifetime(){for(var e:TRAIL.getAsJsonArray("m_Initializers")){var n=e.getAsJsonObject();if(n.has("m_nOutputField")&&n.get("m_nOutputField").getAsInt()==1)return n.getAsJsonObject("m_InputValue").get("m_flLiteralValue").getAsDouble();}throw new IllegalStateException("Missing original tracer lifetime");}
    public static void draw(WorldRenderContext context){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null)return;
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);Vec3 camera=context.worldState().cameraRenderState.pos;
        var buffer=context.consumers();VertexConsumer vertices=null;
        PoseStack.Pose pose=context.matrices().last();
        JsonArray tint=TRAIL.getAsJsonArray("m_ConstantColor");double life=lifetime(),radius=TRAIL.get("m_flConstantRadius").getAsDouble()*Retail.SOURCE_UNIT;
        for(ClientState state:HazeClient.STATES.values())if(state.active)for(double[] effect:state.effects){
            if(effect[8]>0)continue;double age=state.time+partial*0.05-effect[1];if(age<0||age>=life)continue;
            Vec3 from=new Vec3(effect[2],effect[3],effect[4]),to=new Vec3(effect[5],effect[6],effect[7]);Vec3 direction=to.subtract(from);
            if(direction.lengthSqr()<1e-9)continue;
            Vec3 side=direction.cross(camera.subtract(from)).normalize().scale(radius*(1-age/life));
            double t=age/life;int red=(int)(tint.get(0).getAsInt()*(1-t)+33*t),green=(int)(tint.get(1).getAsInt()*(1-t)+20*t),blue=(int)(tint.get(2).getAsInt()*(1-t)+18*t),alpha=(int)(255*(1-t));
            if(vertices==null)vertices=buffer.getBuffer(TYPE);
            vertex(vertices,pose,from.add(side).subtract(camera),0,0,red,green,blue,alpha);vertex(vertices,pose,to.add(side).subtract(camera),1,0,red,green,blue,alpha);
            vertex(vertices,pose,to.subtract(side).subtract(camera),1,1,red,green,blue,alpha);vertex(vertices,pose,from.subtract(side).subtract(camera),0,1,red,green,blue,alpha);
        }
        if(vertices!=null&&buffer instanceof MultiBufferSource.BufferSource source)source.endBatch(TYPE);
    }
    private static void vertex(VertexConsumer v,PoseStack.Pose pose,Vec3 p,float u,float w,int r,int g,int b,int a){v.addVertex(pose,(float)p.x,(float)p.y,(float)p.z).setColor(r,g,b,a).setUv(u,w).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,1,0);}
}
