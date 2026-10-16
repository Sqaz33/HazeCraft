package dev.local.haze.sim;

import java.util.*;

/** Crop glyph geometry in label space, before its parent rotation/scale. */
public final class LocalTextClip {
    public record Vertex(float x,float y,float z,float u,float v) {
        Vertex lerp(Vertex b,float t){return new Vertex(x+(b.x-x)*t,y+(b.y-y)*t,z+(b.z-z)*t,u+(b.u-u)*t,v+(b.v-v)*t);}
    }
    public static float cutoff(float textLeft,float textWidth,float padding,float fraction){
        float fill=Math.max(0,Math.min(1,fraction));
        return textLeft-padding+(textWidth+2*padding)*fill;
    }
    public static List<Vertex> right(List<Vertex> polygon,float cutoff) {
        if(polygon.isEmpty())return List.of();
        List<Vertex> result=new ArrayList<>(polygon.size()+2);Vertex previous=polygon.getLast();
        for(Vertex current:polygon){
            boolean a=previous.x<=cutoff,b=current.x<=cutoff;
            if(a!=b)result.add(previous.lerp(current,(cutoff-previous.x)/(current.x-previous.x)));
            if(b)result.add(current);previous=current;
        }
        return result.size()<3?List.of():List.copyOf(result);
    }
}
