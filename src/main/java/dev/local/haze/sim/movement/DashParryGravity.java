package dev.local.haze.sim.movement;

import dev.local.haze.sim.SourceMath;

/** A modifier value: absence and a present identity scale remain distinct. */
public record DashParryGravity(boolean active, float scale) {
    public static DashParryGravity at(float cancelTime, float time, float duration, float initialScale) {
        float elapsed=time-cancelTime;
        if(cancelTime==0 || duration<=0 || elapsed>=duration)
            return new DashParryGravity(false,0);
        return new DashParryGravity(true,SourceMath.remapClamped(elapsed,0,duration,initialScale,1));
    }
}
