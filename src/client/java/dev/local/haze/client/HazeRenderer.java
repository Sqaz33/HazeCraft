package dev.local.haze.client;

import dev.local.haze.sim.*;
import dev.local.haze.sim.movement.MeleeVisuals;
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
    private static HazeMeshRenderer[] meshes;
    private static final Map<UUID,LocomotionPlayback> locomotion=new HashMap<>();
    private static final Map<String,RenderType> TYPES=new HashMap<>();
    private static final RenderPipeline PIPELINE=RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(Identifier.fromNamespaceAndPath("haze","pipeline/skinned"))
        .withVertexFormat(DefaultVertexFormat.NEW_ENTITY,VertexFormat.Mode.TRIANGLES)
        .withSampler("Sampler1").withCull(false).withBlend(BlendFunction.TRANSLUCENT)
        .withShaderDefine("ALPHA_CUTOUT",0.1F).build();
    public record ActorFrame(Vec3 position,float yaw,SkinnedAsset.Geometry[] geometry,int light,int alpha,float scale) {}
    public record Label(Vec3 position,int count) {}
    private static List<ActorFrame> frames=List.of();
    private static List<Label> labels=List.of();
    public static String currentClip="";
    public static long renderedFrames;
    public static double currentBodyOffset;
    public static void extract(WorldExtractionContext context) {
        long measured=HazePerformance.frame();
        Minecraft mc=Minecraft.getInstance();if(mc.level==null||mc.player==null){frames=List.of();labels=List.of();locomotion.clear();return;}
        if(HazeClient.STATES.values().stream().noneMatch(s->s.active)){frames=List.of();labels=List.of();locomotion.clear();return;}
        locomotion.keySet().removeIf(id->!HazeClient.STATES.containsKey(id)||!HazeClient.STATES.get(id).active);
        if(asset==null){
            asset=SkinnedAsset.shared();
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
            if(clip.contains("reload")&&asset.clips.containsKey(clip))time=asset.clips.get(clip).duration()*Math.min(1,(state.reloadDuration-state.reload+partial*0.05)/state.reloadDuration);
            if(clip.contains("dash")&&motion!=null)time=motion.time-(motion.mode==Motion.Mode.AIR&&motion.jumpKind==Motion.JumpKind.WALL?motion.jumpStart:motion.dashStart)+partial*0.05;
            if(clip.contains("mantle")&&motion!=null)time=motion.time-motion.mantleStart+partial*0.05;
            if((clip.endsWith("jump_ground")||clip.endsWith("jump_invis_start"))&&motion!=null)time=motion.time-motion.jumpStart+partial*0.05;
            if(clip.contains("slide")&&motion!=null){time=motion.time-motion.slideStart+partial*0.05;if(clip.endsWith("slide_loop"))time-=asset.clips.get("models/heroes_staging/haze/clips/slide_start").duration();}
            if(clip.contains("ability")||clip.contains("melee")||clip.contains("parry"))time=state.actionTime+partial*0.05;
            if(clip.contains("melee")){
                String phase=motion!=null?motion.melee.phase.name():state.meleePhase;
                double age=(motion!=null?motion.melee.age():state.meleeAge)+partial*.05;
                if(phase.equals("CHARGING"))time=Math.max(0,age-Retail.DATA.field("ability_melee_haze","m_flChargeAnimDelayTime"));
                else if(phase.endsWith("DASH"))time=asset.clips.get(clip).duration()*Math.min(1,age/(motion!=null?motion.melee.movementDuration():Retail.DATA.ability("ability_melee_haze").getAsJsonObject("m_mapAttacks").getAsJsonObject(state.meleeType.equals("HEAVY_AIR")?"EAttackType_HeavyAir":state.meleeType.equals("SLIDE")?"EAttackType_Slide":"EAttackType_Heavy").getAsJsonObject("m_MovementSpeedCurve").getAsJsonArray("m_vDomainMaxs").get(0).getAsDouble()));
                else time=(motion!=null?motion.melee.time-motion.melee.strikeTime:state.meleeStrikeAge)+partial*.05;
            }
            if(!asset.clips.containsKey(clip))clip="primary_idle";
            List<SkinnedAsset.Layer> locomotionLayers=List.of();
            if(motion!=null&&clip.contains("_run_")){
                String family=clip.substring(clip.lastIndexOf('/')+1,clip.lastIndexOf('_'));
                var blended=locomotion.computeIfAbsent(state.uuid,id->new LocomotionPlayback()).pose(asset,family,motion,entity.getYRot(partial),partial);
                clip=blended.clip();time=blended.time();locomotionLayers=blended.layers();
            }else locomotion.remove(state.uuid);
            currentClip=clip;
            boolean loop=!clip.contains("dash")&&!clip.contains("mantle")&&!clip.contains("melee")&&!clip.contains("parry")&&!clip.endsWith("jump_ground")&&!clip.endsWith("jump_invis_start")&&!clip.endsWith("slide_start");
            boolean running=motion!=null&&motion.velocity.horizontal()>0.2;
            List<SkinnedAsset.Layer> layers=new ArrayList<>(locomotionLayers);
            layers.addAll(HazePose.layers(asset,clip,time,loop,motion==null?Motion.Mode.IDLE:motion.mode,state.smoke,motion!=null&&motion.crouchedPose(),running,
                entity.getXRot(partial),0,state.daggerTime+partial*0.05,state.reload/state.reloadDuration*Retail.number(Retail.DATA.weapon().get("m_reloadDuration")),
                state.time-state.rightShot+partial*0.05,state.time-state.leftShot+partial*0.05));
            var pose=asset.pose(clip,time,loop,layers);
            if(own)for(int bone=0;bone<asset.nodes.length;bone++)if(asset.nodes[bone].name().equals("pelvis")){var root=pose[bone].getTranslation(new org.joml.Vector3f());currentBodyOffset=Math.hypot(root.x,root.z);break;}
            SkinnedAsset.Geometry[] geometry=asset.skin(pose);
            HazeSounds.animation(state.uuid,entity,clip,time,loop);
            for(var layer:layers)if(layer.clip().contains("reload")||layer.clip().contains("dagger"))HazeSounds.animation(state.uuid,entity,"action",layer.clip(),layer.time(),layer.loop());
            Vec3 pos=new Vec3(entity.xOld+(entity.getX()-entity.xOld)*partial,entity.yOld+(entity.getY()-entity.yOld)*partial,entity.zOld+(entity.getZ()-entity.zOld)*partial);
            int light=LevelRenderer.getLightColor(mc.level,entity.blockPosition());
            float modelYaw=entity.getYRot(partial);
            if(clip.contains("melee")&&(motion!=null?motion.melee.locksMovement():!state.meleePhase.equals("NONE")&&!state.meleePhase.equals("CHARGING")))modelYaw=motion!=null?motion.melee.yaw:state.meleeYaw;
            if(motion!=null&&(motion.mode==Motion.Mode.DASH_GROUND||motion.mode==Motion.Mode.DASH_AIR))
                modelYaw=DashVisuals.yaw(clip,motion.dashDirection,modelYaw);
            actors.add(new ActorFrame(pos,modelYaw,geometry,light,state.invisible?100:255,(float)state.itemModelScale));
            for(ClientState.Stack stack:state.stacks) {
                Entity mob=mc.level.getEntity(stack.id());if(mob==null||(!stack.sleep()&&stack.count()==0))continue;
                if(stack.count()>0)text.add(new Label(mob.position().add(0,mob.getBbHeight()+0.35,0),stack.count()));
            }
        }
        frames=List.copyOf(actors);labels=List.copyOf(text);
        HazePerformance.extracted(measured);
    }
    public static void closeMeshes(){if(meshes!=null){for(var mesh:meshes)mesh.close();meshes=null;}}
    private static String chooseClip(ClientState s,Motion m,Controls c) {
        String prefix="models/heroes_staging/haze/clips/";
        if(s.flurry)return prefix+"ability_bullet_flurry";
        if(s.action.startsWith("parry")&&s.actionTime<0.75)return prefix+s.action;
        String meleeClip=m!=null?MeleeVisuals.clip(m.melee.phase.name(),m.melee.type.name(),m.melee.age(),m.melee.time-m.melee.strikeTime,m.melee.chargeDelay(),m.grounded):
            MeleeVisuals.clip(s.meleePhase,s.meleeType,s.meleeAge,s.meleeStrikeAge,Retail.DATA.field("ability_melee_haze","m_flChargeAnimDelayTime"),true);
        if(meleeClip!=null)return prefix+meleeClip;
        if(m!=null) {
            switch(m.mode) {
                case DASH_GROUND: return prefix+"dash_ground";
                case DASH_AIR: return AnimationSource.movementClip(s.smoke,"dash_air_"+m.dashFacing);
                case SLIDE: return prefix+(m.time-m.slideStart<asset.clips.get(prefix+"slide_start").duration()?"slide_start":"slide_loop");
                case MANTLE: return prefix+"mantle_"+m.mantleHeight;
                case AIR:
                    String jump=AnimationSource.movementClip(s.smoke,m.jumpKind==Motion.JumpKind.WALL?"dash_air_"+m.jumpFacing:
                        m.jumpKind==Motion.JumpKind.AIR?"jump_air":m.jumpKind==Motion.JumpKind.DASH?"jump_dash":"jump_ground");
                    return m.time-m.jumpStart<asset.clips.get(jump).duration()?jump:AnimationSource.movementClip(s.smoke,"in_air_loop_down");
                default: break;
            }
            if(m.velocity.horizontal()>0.2) {
                String dir=c.down(Controls.FORWARD)?(c.down(Controls.LEFT)?"nw":c.down(Controls.RIGHT)?"ne":"n"):
                    c.down(Controls.BACK)?(c.down(Controls.LEFT)?"sw":c.down(Controls.RIGHT)?"se":"s"):c.down(Controls.LEFT)?"w":c.down(Controls.RIGHT)?"e":"n";
                return prefix+(s.smoke?"invis_run_":m.mode==Motion.Mode.SPRINT?"out_of_combat_run_":m.crouchedPose()?"weapon_crouch_run_":"weapon_run_")+dir;
            }
        }
        boolean crouch=m!=null&&m.crouchedPose();
        return prefix+(s.smoke?(crouch?"invis_crouch_idle":"invis_stand_idle"):crouch?"weapon_crouch_idle":"weapon_stand_idle");
    }
    private static RenderType type(String texture) {
        return TYPES.computeIfAbsent(texture,t->RenderType.create("haze_"+t,RenderSetup.builder(PIPELINE)
            .withTexture("Sampler0",Identifier.fromNamespaceAndPath("haze","retail/"+t)).useLightmap().useOverlay().bufferSize(4194304).createRenderSetup()));
    }
    public static void draw(WorldRenderContext context) {
        if(asset==null||frames.isEmpty())return;
        long measured=HazePerformance.start();
        if(meshes==null){meshes=new HazeMeshRenderer[asset.primitives.length];for(int p=0;p<meshes.length;p++)meshes[p]=new HazeMeshRenderer(asset.primitives[p]);}
        PoseStack matrices=context.matrices();MultiBufferSource consumers=context.consumers();
        Vec3 camera=context.worldState().cameraRenderState.pos;
        for(ActorFrame frame:frames) {
            matrices.pushPose();matrices.translate(frame.position.x-camera.x,frame.position.y-camera.y,frame.position.z-camera.z);
            // Neutral aim is the middle of the heading sweep: the original model faces +Z.
            matrices.mulPose(Axis.YP.rotationDegrees(-frame.yaw));matrices.scale(frame.scale,frame.scale,frame.scale);
            for(int p=0;p<asset.primitives.length;p++) {
                SkinnedAsset.Primitive primitive=asset.primitives[p];SkinnedAsset.Geometry geometry=frame.geometry[p];
                if(!HazePerformance.reference){meshes[p].draw(type(primitive.texture()),matrices.last(),primitive,geometry,frame.light,frame.alpha);continue;}
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
        if(HazePerformance.reference&&consumers instanceof MultiBufferSource.BufferSource source) for(RenderType type:TYPES.values())source.endBatch(type);
        Minecraft mc=Minecraft.getInstance();
        float partial=mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        for(Label label:labels) {
            matrices.pushPose();matrices.translate(label.position.x-camera.x,label.position.y-camera.y,label.position.z-camera.z);
            matrices.mulPose(context.worldState().cameraRenderState.orientation);
            float pixel=(float)(label.position.distanceTo(camera)*2*Math.tan(Math.toRadians(HazeView.verticalFov(partial))/2)/1080);
            matrices.scale(pixel,-pixel,pixel);
            statusImage(matrices,consumers,"hud/world_space/status_bg_psd.png",-15,-30,30,0xffffffff,0);
            statusImage(matrices,consumers,"hud/world_space/status_inner_psd.png",-15,-30,30,(HudSource.color("hazeColor")&0xffffff)|0x80000000,-0.01F);
            statusImage(matrices,consumers,"hud/modifiers/haze_fixation_png.png",-9,-24,18,HudSource.color("StatusIconColor"),-0.02F);
            statusImage(matrices,consumers,"hud/world_space/status_border.png",-15,-30,30,0x66ffefd7,-0.03F);
            var number=net.minecraft.network.chat.Component.literal(""+label.count).withStyle(net.minecraft.network.chat.Style.EMPTY.withFont(new net.minecraft.network.chat.FontDescription.Resource(Identifier.fromNamespaceAndPath("haze","oracle"))));
            matrices.translate(13,-14,-0.04);matrices.scale(14F/32,14F/32,1);
            mc.font.drawInBatch(number,-mc.font.width(number),0,HudSource.color("offWhite"),true,matrices.last().pose(),consumers,Font.DisplayMode.NORMAL,0,15728880);
            matrices.popPose();
        }
        if(!labels.isEmpty()&&consumers instanceof MultiBufferSource.BufferSource source)source.endBatch();
        renderedFrames++;
        HazePerformance.drawn(measured);
    }
    private static void statusImage(PoseStack matrices,MultiBufferSource buffer,String texture,float x,float y,float size,int tint,float z){
        VertexConsumer v=buffer.getBuffer(RenderTypes.entityTranslucentEmissive(Identifier.fromNamespaceAndPath("haze","retail/"+texture),false));var pose=matrices.last();
        v.addVertex(pose,x,y,z).setColor(tint).setUv(0,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,0,-1);
        v.addVertex(pose,x,y+size,z).setColor(tint).setUv(0,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,0,-1);
        v.addVertex(pose,x+size,y+size,z).setColor(tint).setUv(1,1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,0,-1);
        v.addVertex(pose,x+size,y,z).setColor(tint).setUv(1,0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(15728880).setNormal(pose,0,0,-1);
    }
}

