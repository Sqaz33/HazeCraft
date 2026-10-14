package dev.local.haze.client;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class HudSource {
    public static final JsonObject DATA=load();
    private static JsonObject load(){
        try(InputStream in=HudSource.class.getResourceAsStream("/haze/retail/hud.json")){
            if(in==null)throw new IllegalStateException("Missing local Panorama extraction");
            return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    public static String rule(String sheet,String selector,String property){
        return DATA.getAsJsonObject("sheets").getAsJsonObject(sheet).getAsJsonObject(selector).get(property).getAsString();
    }
    public static double px(String sheet,String selector,String property){return Double.parseDouble(rule(sheet,selector,property).replace("px","").replace("%",""));}
    public static String loc(String key){JsonElement v=DATA.getAsJsonObject("localization").get(key);return v==null?key:v.getAsString();}
    public static int color(String value){
        JsonObject defines=DATA.getAsJsonObject("defines");
        if(defines.has(value))return color(defines.get(value).getAsString());
        if(value.equals("white"))return 0xffffffff;if(value.equals("black"))return 0xff000000;
        if(value.startsWith("#")){
            String hex=value.substring(1);
            if(hex.length()==6)return (int)(0xff000000L|Long.parseLong(hex,16));
            long rgba=Long.parseLong(hex,16);return(int)((rgba>>>8)|((rgba&255)<<24));
        }
        throw new IllegalArgumentException("Unresolved retail HUD color: "+value);
    }
    public static int[] size(String path){JsonArray a=DATA.getAsJsonObject("imageSizes").getAsJsonArray(path);if(a==null)throw new IllegalArgumentException("Missing retail image "+path);return new int[]{a.get(0).getAsInt(),a.get(1).getAsInt()};}
}
