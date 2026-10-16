package dev.local.haze.client;

import dev.local.haze.sim.*;

/** Locomotion follows the local interpolated timeline, independently of snapshot delivery. */
final class LocomotionPlayback {
    private Motion owner;
    private double tick=Double.NaN,clock=Double.NaN,phase;
    private V3 previous=V3.ZERO,current=V3.ZERO;
    LocomotionBlend.Pose pose(SkinnedAsset asset,String family,Motion motion,double yaw,float partial) {
        if(owner!=motion){owner=motion;tick=Double.NaN;clock=Double.NaN;phase=0;previous=current=motion.velocity;}
        if(tick!=motion.time){previous=current;current=motion.velocity;tick=motion.time;}
        V3 velocity=previous.mul(1-partial).add(current.mul(partial));
        double now=motion.time+partial*.05;
        double duration=LocomotionBlend.duration(asset,family);
        if(Double.isFinite(clock))phase+=Math.max(0,now-clock)/duration;
        clock=now;
        return LocomotionBlend.poseAtPhase(asset,family,velocity,yaw,phase);
    }
}
