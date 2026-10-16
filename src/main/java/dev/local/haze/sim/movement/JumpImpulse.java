package dev.local.haze.sim.movement;

import dev.local.haze.sim.V3;

/** Current native air-jump arithmetic. Inputs share Source length units; Java Y is up. */
public final class JumpImpulse {
    public static float bonusScale(float percentage) { return Math.max(0,(percentage+100)*.01f); }
    public static V3 air(V3 velocity,V3 input,float maxWishSpeed,float maxSpeedDelta,
            float verticalSpeed,float verticalBonusPercent,float movementScale) {
        float vx=(float)velocity.x(),vz=(float)velocity.z();
        float wx=Math.clamp((float)input.x(),-1,1),wz=Math.clamp((float)input.z(),-1,1);
        if(wx!=0||wz!=0) {
            float missing=maxWishSpeed-(vx*wx+vz*wz);
            float delta=Math.min(maxSpeedDelta,missing);
            if(delta>0){vx=vx+delta*wx;vz=delta*wz+vz;}
        }
        return new V3(vx,bonusScale(verticalBonusPercent)*verticalSpeed*movementScale,vz);
    }
}
