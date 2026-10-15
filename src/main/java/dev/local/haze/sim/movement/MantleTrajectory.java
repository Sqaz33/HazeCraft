package dev.local.haze.sim.movement;

import dev.local.haze.sim.SourceMath;
import dev.local.haze.sim.V3;

/** Numeric trajectory only; probe selection, execution and collision belong to the controller. */
public record MantleTrajectory(V3 start, V3 animationEnd, float startTime,
        float verticalTime, float horizontalTime, float tickInterval,
        float frontCorrection, float upCorrection) {
    public MantleTrajectory {
        if (!start.finite() || !animationEnd.finite() || !Float.isFinite(startTime)
                || !Float.isFinite(tickInterval) || tickInterval<=0
                || !Float.isFinite(verticalTime) || !Float.isFinite(horizontalTime)
                || verticalTime<tickInterval*.5f || horizontalTime<tickInterval*.5f
                || !Float.isFinite(frontCorrection) || !Float.isFinite(upCorrection))
            throw new IllegalArgumentException("Invalid mantle trajectory");
    }
    public float roundedVerticalTime() { return Math.round(verticalTime/tickInterval)*tickInterval; }
    public float roundedHorizontalTime() { return Math.round(horizontalTime/tickInterval)*tickInterval; }
    /** Coordinates may use any common length unit; V3's Y axis is up. */
    public V3 sample(float time) {
        float elapsed=Math.max(time,startTime)-startTime;
        float vertical=roundedVerticalTime(),horizontal=roundedHorizontalTime();
        float v=Math.clamp(elapsed/vertical,0,1);
        float h=Math.clamp((elapsed-vertical)/horizontal,0,1);
        float correction=SourceMath.bias(Math.clamp(elapsed/(vertical*.5f),0,1),.7f);
        float sx=(float)start.x(),sy=(float)start.y(),sz=(float)start.z();
        float dx=(float)animationEnd.x()-sx,dy=(float)animationEnd.y()-sy,dz=(float)animationEnd.z()-sz;
        float length=(float)Math.sqrt(dx*dx+dz*dz);
        float nx=length==0?0:dx*(1/length),nz=length==0?0:dz*(1/length);
        return new V3(nx*frontCorrection*correction+dx*h+sx,
            dy*v+sy+upCorrection*correction,nz*frontCorrection*correction+dz*h+sz);
    }
}
