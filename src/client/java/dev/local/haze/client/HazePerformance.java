package dev.local.haze.client;

import com.google.gson.*;
import java.util.*;

/** Optional lab-only A/B measurements; disabled in normal launches. */
final class HazePerformance {
    static final boolean ENABLED=Boolean.getBoolean("haze.lab")&&Boolean.getBoolean("haze.perf");
    static boolean reference;
    private static boolean recording;
    private static long previous;
    private static final List<Long> frames=new ArrayList<>(),extract=new ArrayList<>(),draw=new ArrayList<>();
    static long frame(){
        if(!recording)return 0;long now=System.nanoTime();
        if(previous!=0)frames.add(now-previous);previous=now;return now;
    }
    static long start(){return recording?System.nanoTime():0;}
    static void extracted(long start){if(start!=0)extract.add(System.nanoTime()-start);}
    static void drawn(long start){if(start!=0)draw.add(System.nanoTime()-start);}
    static void begin(){frames.clear();extract.clear();draw.clear();previous=0;recording=true;}
    static JsonObject end(){
        recording=false;JsonObject result=new JsonObject();result.addProperty("reference",reference);
        result.add("frame",stats(frames));result.add("extract",stats(extract));result.add("draw",stats(draw));return result;
    }
    private static JsonObject stats(List<Long> values){
        JsonObject result=new JsonObject();result.addProperty("samples",values.size());if(values.isEmpty())return result;
        long[] sorted=values.stream().mapToLong(Long::longValue).sorted().toArray();
        result.addProperty("meanMs",values.stream().mapToLong(Long::longValue).average().orElse(0)/1e6);
        result.addProperty("medianMs",sorted[sorted.length/2]/1e6);result.addProperty("p95Ms",sorted[Math.min(sorted.length-1,(int)(sorted.length*.95))]/1e6);return result;
    }
}
