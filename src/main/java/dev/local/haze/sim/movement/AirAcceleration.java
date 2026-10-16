package dev.local.haze.sim.movement;

import dev.local.haze.sim.V3;
import dev.local.haze.sim.SourceMath;

/** Original ordinary air acceleration, after drag. Units are Source lengths. */
public final class AirAcceleration {
    public static V3 apply(V3 velocity,V3 wish,float preparedWishSpeed,float override,float controlSlow,float controlFast,
            float controlMultiplier,float acceleration,float accelerationMultiplier,float dt) {
        float x=(float)velocity.x(),y=(float)velocity.y(),z=(float)velocity.z();
        float wx=(float)wish.x(),wy=(float)wish.y(),wz=(float)wish.z();
        float speed=(float)Math.sqrt(x*x+z*z);
        float blend=SourceMath.remapClamped(speed,600,800,0,1);
        float control=override>0?override:blend*(controlFast-controlSlow)+controlSlow;
        float target=Math.min(preparedWishSpeed,controlMultiplier*control);
        float missing=target-(y*wy+z*wz+wx*x);
        if(missing<=0)return velocity;
        float delta=Math.min(missing,accelerationMultiplier*acceleration*target*dt);
        float length=(float)Math.sqrt(wy*wy+wz*wz+wx*wx);
        if(length==0)return velocity;
        float inverse=1/length;wx*=inverse;wy*=inverse;wz*=inverse;
        return new V3(wx*delta+x,wy*delta+y,wz*delta+z);
    }
}
