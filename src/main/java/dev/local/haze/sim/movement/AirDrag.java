package dev.local.haze.sim.movement;

import dev.local.haze.sim.V3;

/** Horizontal kernel from current native movement; Source units and Java Y-up. */
public final class AirDrag {
    public static V3 apply(V3 velocity,float bodyYaw,float forwardInput,float sideInput,
            float speedOverride,float drag,float stopSpeed,float slowScale,float dt) {
        float x=(float)velocity.x(),z=(float)velocity.z();
        float speed=(float)Math.sqrt(x*x+z*z);
        if(speed<1e-6f)return velocity;
        float half=(float)Math.toRadians(bodyYaw)/2;
        float qz=(float)Math.sin(half),qw=(float)Math.cos(half);
        float fx=1-(qz+qz)*qz,fz=qw*(qz+qz);
        float length=(float)Math.sqrt(fx*fx+fz*fz);fx/=length;fz/=length;
        float along=x*fx+z*fz;
        float px=along*fx,pz=along*fz;
        // The native tangent is made from the projected vector, including its zero case.
        float tx=-pz,tz=px;
        length=(float)Math.sqrt(tx*tx+tz*tz);
        if(length!=0){tx/=length;tz/=length;}
        float lateral=x*tx+z*tz;
        float sx=lateral*tx,sz=lateral*tz;
        float remaining=Math.max(0,speed-Math.max(speed,stopSpeed)*slowScale*drag*dt);
        float ratio=remaining/speed;
        if(speedOverride<speed||(forwardInput==0&&speedOverride>0)){px*=ratio;pz*=ratio;}
        if(speedOverride<speed||(sideInput==0&&speedOverride>0)){sx*=ratio;sz*=ratio;}
        return new V3(sx+px,velocity.y(),sz+pz);
    }
}
