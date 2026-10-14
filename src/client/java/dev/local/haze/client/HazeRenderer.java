package dev.local.haze.client;

import dev.local.haze.sim.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.client.rendering.v1.world.*;
import com.mojang.blaze3d.pipeline.*;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import java.util.*;

public final class HazeRenderer {
    private static SkinnedAsset asset;
    private static final Map<String,RenderType> TYPES=new HashMap<>();
    private static final Map<String,Map<String,Float>> MASKS=new HashMap<>();
    private static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("haze","pipeline/skinned"))
        .withVertexFormat(DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.TRIANGLES)
        .withSampler("Sampler1").withCull(false).withBlend(BlendFunction.TRANSLUCENT)
        .withShaderDefine("ALPHA_CUTOUT",0.1F).build();
    public record ActorFrame(Vec3 position,float yaw,SkinnedAsset.Geometry[] geometry,int light,int alpha) {}
    public record Label(Vec3 position,String text) {}
    private static List<ActorFrame> frames=List.of();
    private static List<Label> labels=List.of();
    public static String currentClip="";
    public static long renderedFrames;
    public static void extract(WorldExtractionContext context) {
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null){frames=List.of();labels=List.of();return;}
        if(HazeClient.STATES.values().stream().noneMatch(s->s.active)){frames=List.of();labels=List.of();return;}
        if(asset==null){
            asset=SkinnedAsset.shared();
            HudSource.DATA.getAsJsonObject("masks").entrySet().forEach(entry->{Map<String,Float> weights=new HashMap<>();entry.getValue().getAsJsonObject().entrySet().forEach(bone->weights.put(bone.getKey(),bone.getValue().getAsFloat()));MASKS.put(entry.getKey(),Map.copyOf(weights));});
            dev.local.haze.HazeMod.LOG.info("Loaded original Haze: {} clips, {} primitives",asset.clips.size(),asset.primitives.length);
        }
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        List<ActorFrame> actors=new ArrayList<>();List<Label> text=new ArrayList<>();
        for(ClientState state:HazeClient.STATES.values()) {
            if(!state.active)continue;Entity entity=mc.level.getEntity(state.entityId);if(entity==null)continue;
            boolean own=entity==mc.player;
            Motion motion=own?HazeClient.motion:null;
            String clip=chooseClip(state,motion,own?HazeClient.controls:Controls.NONE);
            double time=state.time+partial*0.05;
            if(clip.contains("reload")&&asset.clips.containsKey(clip))time=asset.clips.get(clip).duration()*Math.min(1,(Retail.DATA.weapon().get("m_reloadDuration").getAsDouble()-state.reload+partial*0.05)/Retail.DATA.weapon().get("m_reloadDuration").getAsDouble());
            if(clip.contains("dash")&&motion!=null)time=motion.time-motion.dashStart+partial*0.05;
            if(clip.contains("ability")||clip.contains("melee")||clip.contains("parry"))time=state.actionTime+partial*0.05;
            if(!asset.clips.containsKey(clip))clip="primary_idle";
            currentClip=clip;
            List<SkinnedAsset.Layer> layers=new ArrayList<>();
            if(!state.flurry&&state.reload<=0&&!clip.contains("melee")&&!clip.contains("ability")&&!clip.contains("dash")&&!clip.contains("mantle")){
                String aim="models/heroes_staging/haze/clips/aim_"+(state.smoke?"invis":"weapon")+"_idle";
                float pitch=entity.getXRot(partial);
                layers.add(new SkinnedAsset.Layer(aim,0,false,1,null,true,aim+(pitch>0?"_down":"_up"),0,Math.min(1,Math.abs(pitch)/90F)));
            }
            if(!state.flurry&&state.reload<=0&&!clip.contains("melee")&&!clip.contains("ability")){
                boolean running=motion!=null&&motion.velocity.horizontal()>0.2;
                for(int hand=0;hand<2;hand++){
                    double since=state.time-(hand==0?state.rightShot:state.leftShot)+partial*0.05;
                    String shoot="models/heroes_staging/haze/clips/shoot_"+(running?"run_":"idle_")+(hand==0?"r":"l");
                    if(asset.clips.containsKey(shoot)&&since>=0&&since<asset.clips.get(shoot).duration())layers.add(new SkinnedAsset.Layer(shoot,since,false,1,MASKS.get(hand==0?"shoot_R":"shoot_L"),true));
                }
            }
            SkinnedAsset.Geometry[] geometry=asset.skin(asset.pose(clip,time,!clip.contains("reload")&&!clip.contains("dagger")&&!clip.contains("melee")&&!clip.contains("parry"),layers));
            Vec3 pos=new Vec3(entity.xOld+(entity.getX()-entity.xOld)*partial,entity.yOld+(entity.getY()-entity.yOld)*partial,entity.zOld+(entity.getZ()-entity.zOld)*partial);
            int light=LevelRenderer.getLightColor(mc.level,entity.blockPosition());
            actors.add(new ActorFrame(pos,entity.getYRot(partial),geometry,light,state.invisible?100:255));
            for(ClientState.Stack stack:state.stacks) {
                Entity mob=mc.level.getEntity(stack.id());if(mob==null||(!stack.sleep()&&stack.count()==0))continue;
                String line=(stack.count()>0?"Fixation ×"+stack.count():"")+(stack.sleep()?"  Zzz":"");
                text.add(new Label(mob.position().add(0,mob.getBbHeight()+0.35,0),line));
            }
        }
        frames=List.copyOf(actors);labels=List.copyOf(text);
    }
    private static String chooseClip(ClientState s,Motion m,Controls c) {
        String prefix="models/heroes_staging/haze/clips/";
        if(s.flurry)return prefix+"ability_bullet_flurry";
        if(s.action.equals("dagger")&&s.actionTime<0.6)return prefix+"ability_sleep_dagger";
        if(s.action.startsWith("parry")&&s.actionTime<0.75)return prefix+s.action;
        if(s.action.equals("melee_light")&&s.actionTime<0.5)return prefix+"melee_quick_1";
        if(s.action.equals("melee_heavy")&&s.actionTime<0.6)return prefix+"melee_hit";
        if(s.reload>0)return prefix+"reload_idle";
        if(m!=null) {
            switch(m.mode) {
                case DASH_GROUND: return prefix+"dash_ground";
                case DASH_AIR: return prefix+"dash_air_"+(c.down(Controls.LEFT)?"left":c.down(Controls.RIGHT)?"right":c.down(Controls.BACK)?"back":"forward");
                case SLIDE: return prefix+"slide_loop";
                case MANTLE: return prefix+"mantle_"+m.mantleHeight;
                case ROPE: return prefix+(m.velocity.y()>0?"rope_climb_up":m.velocity.y()<0?"rope_climb_down":"rope_climb_idle");
                case ZIPLINE: return "zipline_loop";
                case AIR: return prefix+"in_air_loop_down";
                default: break;
            }
            if(m.velocity.horizontal()>0.2) {
                String dir=c.down(Controls.FORWARD)?(c.down(Controls.LEFT)?"nw":c.down(Controls.RIGHT)?"ne":"n"):
                    c.down(Controls.BACK)?(c.down(Controls.LEFT)?"sw":c.down(Controls.RIGHT)?"se":"s"):c.down(Controls.LEFT)?"w":c.down(Controls.RIGHT)?"e":"n";
                return prefix+(s.smoke?"invis_run_":c.down(Controls.CROUCH)?"weapon_crouch_run_":"weapon_run_")+dir;
            }
        }
        return prefix+(s.smoke?(c.down(Controls.CROUCH)?"invis_crouch_idle":"invis_stand_idle"):c.down(Controls.CROUCH)?"weapon_crouch_idle":"weapon_stand_idle");
    }
    private static RenderType type(String texture) {
        return TYPES.computeIfAbsent(texture,t->RenderType.create("haze_"+t,RenderSetup.builder(PIPELINE)
            .withTexture("Sampler0",Identifier.fromNamespaceAndPath("haze","retail/"+t)).useLightmap().useOverlay().bufferSize(4194304).createRenderSetup()));
    }
    public static void draw(WorldRenderContext context) {
        if(asset==null||frames.isEmpty())return;
        PoseStack matrices=context.matrices();MultiBufferSource consumers=context.consumers();
        Vec3 camera=context.worldState().cameraRenderState.pos;
        for(ActorFrame frame:frames) {
            matrices.pushPose();matrices.translate(frame.position.x-camera.x,frame.position.y-camera.y,frame.position.z-camera.z);
            // Retail muzzle attachment tests establish that the export faces -X.
            matrices.mulPose(Axis.YP.rotationDegrees(90-frame.yaw));
            for(int p=0;p<asset.primitives.length;p++) {
                SkinnedAsset.Primitive primitive=asset.primitives[p];SkinnedAsset.Geometry geometry=frame.geometry[p];
                VertexConsumer v=consumers.getBuffer(type(primitive.texture()));PoseStack.Pose pose=matrices.last();
                float[] pos=geometry.position(),normal=geometry.normal(),uv=primitive.vertices().uv();
                for(int index:primitive.indices()) {
                    int i=index*3,u=index*2;
                    v.addVertex(pose,pos[i],pos[i+1],pos[i+2]).setColor(255,255,255,frame.alpha)
                        .setUv(uv[u],uv[u+1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(frame.light)
                        .setNormal(pose,normal[i],normal[i+1],normal[i+2]);
                }
            }
            matrices.popPose();
        }
        if(consumers instanceof MultiBufferSource.BufferSource source) for(RenderType type:TYPES.values())source.endBatch(type);
        Minecraft mc=Minecraft.getInstance();
        for(Label label:labels) {
            matrices.pushPose();matrices.translate(label.position.x-camera.x,label.position.y-camera.y,label.position.z-camera.z);
            matrices.mulPose(context.worldState().cameraRenderState.orientation);matrices.scale(0.025F,-0.025F,0.025F);
            mc.font.drawInBatch(label.text,-mc.font.width(label.text)/2F,0,0xffffd978,true,matrices.last().pose(),consumers,Font.DisplayMode.NORMAL,0x80000000,15728880);
            matrices.popPose();
        }
        if(consumers instanceof MultiBufferSource.BufferSource source)source.endBatch();
        renderedFrames++;
    }
}

