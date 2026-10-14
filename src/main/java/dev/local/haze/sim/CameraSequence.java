package dev.local.haze.sim;

import com.google.gson.*;

/** Evaluates the explicit retail sequence fields in source camera units. */
public final class CameraSequence {
    private CameraSequence(){}
    public static double evaluate(JsonArray operations,double time,double initial,double defaultValue) {
        if(operations==null)return defaultValue;
        double value=initial,elapsed=time;
        for(JsonElement element:operations) {
            JsonObject op=element.getAsJsonObject();
            double delay=n(op,"m_flDelay",0);if(elapsed<delay)return value;elapsed-=delay;
            boolean relative=b(op,"m_bValuesAreRelative",false);
            String type=op.has("m_eCameraOperation")?op.get("m_eCameraOperation").getAsString():"k_ECameraOp_Approach";
            if(type.equals("k_ECameraOp_Maintain")) {
                double duration=n(op,"m_flMaintainDuration",Double.POSITIVE_INFINITY);
                if(elapsed<duration)return value;elapsed-=duration;continue;
            }
            if(type.equals("k_ECameraOp_Lerp")) {
                double duration=n(op,"m_flLerpDuration",0);
                double start=b(op,"m_bLerpStartAtDefault",false)?defaultValue:value;
                if(op.has("m_flLerpStart")&&!b(op,"m_bLerpStartAtCurrent",false))start=n(op,"m_flLerpStart",0)+(relative?defaultValue:0);
                double target=b(op,"m_bLerpEndAtDefault",true)?defaultValue:n(op,"m_flLerpEnd",0)+(relative?defaultValue:0);
                double t=duration>0?Math.max(0,Math.min(1,elapsed/duration)):1;
                t=bias(t,n(op,"m_flLerpBias",0.5));
                double gain=n(op,"m_flLerpGain",0.5);if(gain!=0.5)t=t<0.5?bias(t*2,1-gain)*0.5:1-bias(2-2*t,1-gain)*0.5;
                value=start+(target-start)*t;
                if(elapsed<duration)return value;elapsed-=duration;continue;
            }
            if(type.equals("k_ECameraOp_Spring")) {
                double target=b(op,"m_bSpringToDefault",true)?defaultValue:n(op,"m_flSpringTarget",0)+(relative?defaultValue:0);
                double strength=n(op,"m_flSpringStrength",0);
                if(strength<=0)return target;
                // Closed form avoids frame-rate-dependent Euler integration.
                double t=Math.max(0,elapsed),decay=Math.exp(-strength*t);
                return target+(value-target)*(1+strength*t)*decay;
            }
            if(type.equals("k_ECameraOp_Approach")) {
                double target=n(op,"m_flApproachTarget",0)+(relative?defaultValue:0);
                double distance=n(op,"m_flApproachSpeed",0)*elapsed+0.5*n(op,"m_flApproachAcceleration",0)*elapsed*elapsed;
                return value+Math.copySign(Math.min(Math.abs(target-value),distance),target-value);
            }
        }
        return value;
    }
    private static double bias(double t,double bias){if(t<=0||t>=1||bias==0.5)return t;return Math.pow(t,Math.log(Math.max(1e-6,Math.min(1-1e-6,bias)))/Math.log(0.5));}
    private static double n(JsonObject o,String k,double fallback){return o.has(k)?Retail.number(o.get(k)):fallback;}
    private static boolean b(JsonObject o,String k,boolean fallback){return o.has(k)?o.get(k).getAsBoolean():fallback;}
}
