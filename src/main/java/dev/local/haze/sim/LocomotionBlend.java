package dev.local.haze.sim;

import com.google.gson.*;
import java.util.*;

/** Triangle and hull topology are the retail Haze blend spaces, in Source units. */
public final class LocomotionBlend {
    public record Sample(String clip,float weight){}
    public record Pose(String clip,double time,List<SkinnedAsset.Layer> layers){}
    public static List<Sample> sample(String family,double x,double y){
        var space=AnimationSource.DATA.getAsJsonObject("locomotion").getAsJsonObject(family);
        if(space==null)throw new IllegalArgumentException("Unknown original locomotion space "+family);
        var points=space.getAsJsonArray("points");var indices=space.getAsJsonArray("triangles");var clips=space.getAsJsonArray("clips");
        var result=inside(points,indices,clips,x,y);if(result!=null)return result;
        var hull=space.getAsJsonArray("hull");double best=Double.POSITIVE_INFINITY,px=0,py=0;
        for(int edge=0;edge<hull.size()-1;edge++){
            var a=points.get(hull.get(edge).getAsInt()).getAsJsonArray();var b=points.get(hull.get(edge+1).getAsInt()).getAsJsonArray();
            double ax=a.get(0).getAsDouble(),ay=a.get(1).getAsDouble(),dx=b.get(0).getAsDouble()-ax,dy=b.get(1).getAsDouble()-ay;
            double t=Math.max(0,Math.min(1,((x-ax)*dx+(y-ay)*dy)/(dx*dx+dy*dy))),nx=ax+dx*t,ny=ay+dy*t,d=(x-nx)*(x-nx)+(y-ny)*(y-ny);
            if(d<best){best=d;px=nx;py=ny;}
        }
        result=inside(points,indices,clips,px,py);if(result==null)throw new IllegalStateException("Retail blend hull is not triangulated");return result;
    }
    private static List<Sample> inside(JsonArray points,JsonArray indices,JsonArray clips,double x,double y){
        for(int triangle=0;triangle<indices.size();triangle+=3){
            int[] ids={indices.get(triangle).getAsInt(),indices.get(triangle+1).getAsInt(),indices.get(triangle+2).getAsInt()};
            double[] px=new double[3],py=new double[3];for(int i=0;i<3;i++){var p=points.get(ids[i]).getAsJsonArray();px[i]=p.get(0).getAsDouble();py[i]=p.get(1).getAsDouble();}
            double denominator=(py[1]-py[2])*(px[0]-px[2])+(px[2]-px[1])*(py[0]-py[2]);
            double a=((py[1]-py[2])*(x-px[2])+(px[2]-px[1])*(y-py[2]))/denominator;
            double b=((py[2]-py[0])*(x-px[2])+(px[0]-px[2])*(y-py[2]))/denominator,c=1-a-b;
            if(a>=-1e-8&&b>=-1e-8&&c>=-1e-8){
                double[] weights={Math.max(0,a),Math.max(0,b),Math.max(0,c)};double total=weights[0]+weights[1]+weights[2];List<Sample> result=new ArrayList<>();
                for(int i=0;i<3;i++)if(weights[i]>1e-7)result.add(new Sample(clips.get(ids[i]).getAsString(),(float)(weights[i]/total)));
                return List.copyOf(result);
            }
        }return null;
    }
    public static Pose pose(SkinnedAsset asset,String family,V3 velocity,double yaw,double clock){
        double x=-velocity.dot(V3.look(yaw+90,0))/Retail.SOURCE_UNIT,y=velocity.dot(V3.look(yaw,0))/Retail.SOURCE_UNIT;
        var samples=sample(family,x,y);var first=samples.getFirst();String reference=AnimationSource.DATA.getAsJsonObject("locomotion").getAsJsonObject(family).getAsJsonArray("clips").get(1).getAsString();
        double duration=asset.clips.get(reference).duration(),baseTime=clock*asset.clips.get(first.clip).duration()/duration;
        float weight=first.weight;List<SkinnedAsset.Layer> layers=new ArrayList<>();
        for(int i=1;i<samples.size();i++){var next=samples.get(i);weight+=next.weight;layers.add(new SkinnedAsset.Layer(next.clip,clock*asset.clips.get(next.clip).duration()/duration,true,next.weight/weight,null,false));}
        return new Pose(first.clip,baseTime,List.copyOf(layers));
    }
}
