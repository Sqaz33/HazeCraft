package dev.local.haze.sim;

import com.google.gson.*;

/** Retail cubic Hermite curve; preserve explicit incoming/outgoing derivatives. */
public final class Curve {
    public static double sample(JsonObject curve,double x) {
        double value=sampleRaw(curve,x);
        if(curve.has("m_vDomainMins"))value=Math.max(value,curve.getAsJsonArray("m_vDomainMins").get(1).getAsDouble());
        if(curve.has("m_vDomainMaxs"))value=Math.min(value,curve.getAsJsonArray("m_vDomainMaxs").get(1).getAsDouble());
        return value;
    }
    private static double sampleRaw(JsonObject curve,double x) {
        JsonArray points=curve.getAsJsonArray("m_spline");
        JsonObject first=points.get(0).getAsJsonObject(),last=points.get(points.size()-1).getAsJsonObject();
        if(x<=n(first,"x")) return n(first,"y");
        if(x>=n(last,"x")) return n(last,"y");
        for(int i=1;i<points.size();i++) {
            JsonObject b=points.get(i).getAsJsonObject();
            if(x>n(b,"x")) continue;
            JsonObject a=points.get(i-1).getAsJsonObject();
            double span=n(b,"x")-n(a,"x"),t=(x-n(a,"x"))/span;
            double t2=t*t,t3=t2*t;
            return (2*t3-3*t2+1)*n(a,"y")+(t3-2*t2+t)*span*n(a,"m_flSlopeOutgoing")
                +(-2*t3+3*t2)*n(b,"y")+(t3-t2)*span*n(b,"m_flSlopeIncoming");
        }
        throw new IllegalStateException();
    }
    private static double n(JsonObject o,String k) { return Retail.number(o.get(k)); }
}
