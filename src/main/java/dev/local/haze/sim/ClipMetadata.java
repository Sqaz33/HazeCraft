package dev.local.haze.sim;
import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
public final class ClipMetadata {
    public static final JsonObject DATA=load();
    public static boolean additive(String clip){var data=DATA.getAsJsonObject(clip);return data!=null&&data.get("additive").getAsBoolean()&&!data.get("additiveComposed").getAsBoolean();}
    private static JsonObject load(){try(var in=ClipMetadata.class.getResourceAsStream("/haze/retail/clip-metadata.json")){return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new UncheckedIOException(e);}}
    public static boolean tag(String clip,double time,boolean loop,String tag){
        var metadata=DATA.getAsJsonObject(clip);if(metadata==null)return false;double duration=metadata.get("duration").getAsDouble();if(loop&&duration>0)time%=duration;
        double frame=duration>0?time/duration*(metadata.get("frames").getAsInt()-1):0;
        for(var e:metadata.getAsJsonArray("events")){var event=e.getAsJsonObject();if(event.has("id")&&event.get("id").getAsString().equals(tag)&&frame>=event.get("frame").getAsDouble()&&frame<event.get("frame").getAsDouble()+event.get("durationFrames").getAsDouble())return true;}
        return false;
    }
}
