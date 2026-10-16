package dev.local.haze.sim.movement;

import dev.local.haze.sim.V3;
import java.util.function.DoubleUnaryOperator;

/** Native wall-jump impulse. All speeds use the same units; Y is up. */
public final class WallJumpImpulse {
    private static V3 norm(V3 v){float x=(float)v.x(),y=(float)v.y(),z=(float)v.z();float l=(float)Math.sqrt(y*y+z*z+x*x);return l==0?V3.ZERO:new V3(x/l,y/l,z/l);}
    public static V3 apply(V3 velocity,V3 wish,V3 contact,float cornerBlend,float minOut,float maxOut,
            float upSpeed,float lateralSpeed,float power,DoubleUnaryOperator falloff) {
        V3 normal=norm(contact);
        // Native flattens by projection, without normalizing the flat component first.
        normal=norm(normal.mul(cornerBlend).add(normal.flat().mul(1-cornerBlend)));
        float nx=(float)normal.x(),ny=(float)normal.y(),nz=(float)normal.z();
        float vx=(float)velocity.x(),vy=(float)velocity.y(),vz=(float)velocity.z();
        float wx=Math.clamp((float)wish.x(),-1,1),wy=Math.clamp((float)wish.y(),-1,1),wz=Math.clamp((float)wish.z(),-1,1);
        float inward=vy*ny+vz*nz+vx*nx;
        if(inward<0){vx-=nx*inward;vz-=inward*nz;vy-=inward*ny;}
        float out=Math.clamp((ny*wy+nz*wz+nx*wx+1)*.5f,0,1)*(maxOut-minOut)+minOut;
        float oldUp=vy<100?vy*.3f:0;
        vx=nx*out+vx;vy=out*ny+oldUp+power*upSpeed;vz=out*nz+vz;
        float tx=-nz,tz=nx;
        float along=tyDot(tx,tz,vx,vy,vz);
        float lateral=Math.clamp((tz*wz+tx*wx+1)*.5f,0,1)*(lateralSpeed-(-lateralSpeed))-lateralSpeed;
        float input=Math.abs(along)*(lateral*along>0?1:-1);
        float scale=Math.clamp((float)falloff.applyAsDouble(input),0,1);
        return new V3(tx*lateral*scale+vx,vy,tz*lateral*scale+vz);
    }
    private static float tyDot(float tx,float tz,float x,float y,float z){return 0*y+tz*z+tx*x;}
}
