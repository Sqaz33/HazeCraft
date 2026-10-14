package dev.local.haze.sim;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bindings and bone weights copied from the installed Haze animation graphs. */
public final class AnimationSource {
    public static final JsonObject DATA=load();
    private static final Map<String,Map<String,Float>> MASKS=new HashMap<>();
    static {DATA.getAsJsonObject("masks").entrySet().forEach(e->{Map<String,Float> weights=new HashMap<>();e.getValue().getAsJsonObject().entrySet().forEach(w->weights.put(w.getKey(),w.getValue().getAsFloat()));MASKS.put(e.getKey(),Map.copyOf(weights));});}
    private static JsonObject load(){try(var in=AnimationSource.class.getResourceAsStream("/haze/retail/animation-graph.json")){if(in==null)throw new IllegalStateException("Missing original animation graph");return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new UncheckedIOException(e);}}
    public static Map<String,Float> mask(String name){var mask=MASKS.get(name);if(mask==null)throw new IllegalArgumentException("Unknown original bone mask: "+name);return mask;}
    public static double headingTime(SkinnedAsset asset,String clip,double heading){
        var range=DATA.getAsJsonObject("headingInput");double min=range.get("m_flBegin").getAsDouble(),max=range.get("m_flEnd").getAsDouble();
        return asset.clips.get(clip).duration()*Math.max(0,Math.min(1,(heading-min)/(max-min)));
    }
    public static SkinnedAsset.Layer aim(SkinnedAsset asset,String variant,String state,double pitch,double heading){
        var bindings=DATA.getAsJsonObject("aims").getAsJsonObject(variant);String name="aim_"+state;
        String clip=bindings.get(name).getAsString(),second=bindings.get(name+(pitch>0?"_down":"_up")).getAsString();
        return new SkinnedAsset.Layer(clip,headingTime(asset,clip,heading),false,1,null,true,second,headingTime(asset,second,heading),(float)Math.min(1,Math.abs(pitch)/90));
    }
    public static SkinnedAsset.Layer dagger(double elapsed){
        float weight=(float)Math.max(0,Math.min(1,elapsed/DATA.get("daggerBlendIn").getAsDouble()));
        return new SkinnedAsset.Layer("models/heroes_staging/haze/clips/ability_sleep_dagger",elapsed,false,weight,mask(DATA.get("daggerMask").getAsString()),false);
    }
}
