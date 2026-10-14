package dev.local.haze.client;

import dev.local.haze.sim.*;
import com.google.gson.*;

/** Camera events use the sequence data embedded in the owned ability definitions. */
public final class HazeView {
    public record Parameters(double back,double side,double height,double fov){}
    public static Parameters parameters(float partial) {
        Retail d=Retail.DATA;ClientState s=HazeClient.own();Motion m=HazeClient.motion;
        boolean crouch=HazeClient.controls.down(Controls.CROUCH),aim=HazeClient.controls.down(Controls.AIM);
        double back=Retail.number(d.camera.get(aim?"m_flCameraBackOffsetAiming":"m_flCameraBackOffset"));
        double side=Retail.number(d.camera.get("m_flCameraSideOffset"));
        double height=Retail.number(d.camera.get(crouch?"m_flCameraHeightCrouching":"m_flCameraHeightStanding"));
        double fov=d.nativeFloat("citadel_camera_hero_fov");
        if(aim)fov+=Retail.number(d.weapon().get("m_flZoomFOV_Relative"));
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
            if(m.mode==Motion.Mode.ZIPLINE) {
                side=Retail.number(d.camera.get("m_flCameraSideOffsetZiplining"));height+=Retail.number(d.camera.get("m_flCameraHeightOffsetZiplining"));
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
