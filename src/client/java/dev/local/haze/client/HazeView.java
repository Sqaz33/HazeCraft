package dev.local.haze.client;

import dev.local.haze.sim.*;
import com.google.gson.*;

/** Camera events use the sequence data embedded in the owned ability definitions. */
public final class HazeView {
    public record Parameters(double back,double side,double height,double fov){}
    private static boolean aiming;
    private static double aimStarted,aimInitial=90,aimFov=90;
    private static final CameraHeightBlend crouchBlend=new CameraHeightBlend((float)Retail.DATA.nativeFloat("citadel_camera_height_approach_speed"));
    private static Motion crouchOwner;
    public static void reset(){aiming=false;aimStarted=0;aimInitial=aimFov=Retail.DATA.nativeFloat("citadel_camera_hero_fov");crouchBlend.reset();crouchOwner=null;}
    /** Advance only during camera setup; HUD/FOV reads must not advance the transition again. */
    public static void updateCrouch(float partial){
        Motion motion=HazeClient.motion;if(motion==null)return;
        if(crouchOwner!=motion){crouchBlend.reset();crouchOwner=motion;}
        crouchBlend.advanceTo(motion.time+partial*.05,motion.crouchedPose());
    }
    public static Parameters parameters(float partial) {
        Retail d=Retail.DATA;ClientState s=HazeClient.own();Motion m=HazeClient.motion;
        boolean crouch=m!=null?m.crouchedPose():HazeClient.controls.down(Controls.CROUCH),aim=HazeClient.controls.down(Controls.AIM);
        double back=Retail.number(d.camera.get("m_flCameraBackOffset"));
        double side=Retail.number(d.camera.get("m_flCameraSideOffset"));
        double height=m!=null?crouchBlend.height(d.camera.get("m_flCameraHeightStanding").getAsFloat(),d.camera.get("m_flCameraHeightCrouching").getAsFloat()):
            Retail.number(d.camera.get(crouch?"m_flCameraHeightCrouching":"m_flCameraHeightStanding"));
        double fov=d.nativeFloat("citadel_camera_hero_fov");
        double clock=(m!=null?m.time:s!=null?s.time:0)+partial*0.05;
        if(aim!=aiming){aiming=aim;aimStarted=clock;aimInitial=aimFov;}
        JsonArray zoom=d.ability("citadel_weapon_haze_set").getAsJsonObject(aim?"m_cameraSequenceZoom":"m_cameraSequenceUnZoom").getAsJsonArray("m_vecFOVOperations").deepCopy();
        double relative=Retail.number(d.weapon().get("m_flZoomFOV_Relative"));
        if(aim)zoom.get(0).getAsJsonObject().addProperty("m_flSpringTarget",relative);
        aimFov=CameraSequence.evaluate(zoom,clock-aimStarted,aimInitial,fov);fov=aimFov;
        double fraction=relative!=0?Math.max(0,Math.min(1,(fov-d.nativeFloat("citadel_camera_hero_fov"))/relative)):aim?1:0;
        back+=(Retail.number(d.camera.get("m_flCameraBackOffsetAiming"))-back)*fraction;
        if(s!=null&&s.action.equals("flurry")) {
            double time=s.actionTime+partial*0.05,delay=d.prop(Retail.SIGNATURES[3],"AbilityCastDelay",s.ranks[3]);
            JsonObject a=d.ability(Retail.SIGNATURES[3]),cast=a.getAsJsonObject("m_cameraSequenceCastStart");
            if(time<delay) {
                back=CameraSequence.evaluate(cast.getAsJsonArray("m_vecDistanceOperations"),time,back,back);
                fov=CameraSequence.evaluate(cast.getAsJsonArray("m_vecFOVOperations"),time,fov,fov);
            }else if(s.flurry) {
                double start=CameraSequence.evaluate(cast.getAsJsonArray("m_vecDistanceOperations"),delay,back,back);
                back=CameraSequence.evaluate(a.getAsJsonObject("m_cameraSequenceChannelStart").getAsJsonArray("m_vecDistanceOperations"),time-delay,start,back);
            }
        }
        if(m!=null) {
            if(m.melee.phase==dev.local.haze.sim.movement.MeleeController.Phase.CHARGING){
                JsonObject hold=d.ability("ability_melee_haze").getAsJsonObject("m_cameraSequenceHoldStart");
                double age=m.melee.chargeAge()+partial*.05;
                back=CameraSequence.evaluate(hold.getAsJsonArray("m_vecDistanceOperations"),age,back,back);
                fov=CameraSequence.evaluate(hold.getAsJsonArray("m_vecFOVOperations"),age,fov,fov);
            }
            if(m.airDashUsed) {
                double elapsed=m.time-m.dashStart+partial*0.05;
                JsonObject dash=d.ability("citadel_ability_dash").getAsJsonObject("m_cameraSequenceAirDashActivate");
                fov=CameraSequence.evaluate(dash.getAsJsonArray("m_vecFOVOperations"),elapsed,fov,fov);
            }
        }
        return new Parameters(back*Retail.SOURCE_UNIT,side*Retail.SOURCE_UNIT,height*Retail.SOURCE_UNIT,fov);
    }
    public static float verticalFov(float partial){return(float)Math.toDegrees(2*Math.atan(Math.tan(Math.toRadians(parameters(partial).fov)/2)/(4.0/3)));}
}
