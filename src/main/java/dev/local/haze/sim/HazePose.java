package dev.local.haze.sim;

import java.util.*;

/** Original additive aim and masked action layers over uninterrupted locomotion. */
public final class HazePose {
    private static final String PREFIX="models/heroes_staging/haze/clips/";
    public static List<SkinnedAsset.Layer> layers(SkinnedAsset asset,String base,double time,boolean loop,
            Motion.Mode mode,boolean smoke,boolean crouch,boolean running,double pitch,double heading,
            double daggerTime,double reloadRemaining,double rightShotAge,double leftShotAge){
        List<SkinnedAsset.Layer> layers=new ArrayList<>();
        boolean fullAction=base.contains("melee")||base.contains("parry")||base.contains("ability_bullet");
        boolean aimOff=fullAction||ClipMetadata.tag(base,time,loop,"TAG_aim_OFF")||mode==Motion.Mode.MANTLE;
        boolean reload=reloadRemaining>0&&!fullAction&&!ClipMetadata.tag(base,time,loop,"TAG_ReloadMask_OFF");
        if(!aimOff&&!reload){
            String state=mode==Motion.Mode.SLIDE?"slide":mode==Motion.Mode.AIR?"in_air":crouch?(running?"crouch_run":"crouch"):running?"run":"idle";
            String variant=smoke?"haze_invisible":base.contains("out_of_combat")?"haze_out_of_combat":"haze";
            layers.add(AnimationSource.aim(asset,variant,state,pitch,heading));
        }
        if(reload){
            double duration=Retail.number(Retail.DATA.weapon().get("m_reloadDuration"));
            double elapsed=asset.clips.get(PREFIX+"reload_idle").duration()*Math.max(0,Math.min(1,(duration-reloadRemaining)/duration));
            String mask=mode==Motion.Mode.SLIDE?"Reload_Slide":mode==Motion.Mode.AIR?"Reload_InAir":crouch?(running?"Reload_HasPathCrouch":"Reload_Crouch"):running?"Reload_Haspath":"Reload_Idle";
            layers.add(new SkinnedAsset.Layer(PREFIX+"reload_idle",elapsed,false,1,AnimationSource.mask(mask),false));
        }
        if(!fullAction&&daggerTime>=0&&daggerTime<asset.clips.get(PREFIX+"ability_sleep_dagger").duration()){
            layers.add(AnimationSource.dagger(daggerTime));
        }else if(!fullAction&&!reload){
            for(int hand=0;hand<2;hand++){
                double age=hand==0?rightShotAge:leftShotAge;
                String shoot=PREFIX+"shoot_"+(running?"run_":"idle_")+(hand==0?"r":"l");
                if(age>=0&&age<asset.clips.get(shoot).duration())layers.add(new SkinnedAsset.Layer(shoot,age,false,1,AnimationSource.mask(hand==0?"shoot_R":"shoot_L"),true));
            }
        }
        return List.copyOf(layers);
    }
}
