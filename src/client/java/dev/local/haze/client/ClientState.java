package dev.local.haze.client;

import com.google.gson.*;
import java.util.*;

public final class ClientState {
    public final UUID uuid;
    public final int entityId;
    public final boolean active,invisible,smoke,flurry,inCombat;
    public final boolean debugNoCooldowns;
    public final double health,maxHealth,reload,time,recoil,bloom,actionTime,daggerTime,leftShot,rightShot,smokeRemaining,danceRemaining;
    public final int ammo,smokeCharges;
    public final long shots;
    public final String action;
    public final int[] ranks=new int[4];
    public final double[] cooldowns=new double[4];
    public final List<Stack> stacks;
    public final double[][] bullets;
    public final double[][] effects;
    public record Stack(int id,int count,boolean sleep) {}
    public ClientState(JsonObject j) {
        uuid=UUID.fromString(j.get("uuid").getAsString());entityId=j.get("id").getAsInt();active=j.get("active").getAsBoolean();
        invisible=j.get("invisible").getAsBoolean();smoke=j.get("smoke").getAsBoolean();flurry=j.get("flurry").getAsBoolean();
        inCombat=j.get("inCombat").getAsBoolean();
        debugNoCooldowns=j.has("debugNoCooldowns")&&j.get("debugNoCooldowns").getAsBoolean();
        health=n(j,"health");maxHealth=n(j,"maxHealth");reload=n(j,"reload");time=n(j,"time");recoil=n(j,"recoil");bloom=n(j,"bloom");actionTime=n(j,"actionTime");
        ammo=j.get("ammo").getAsInt();shots=j.get("shots").getAsLong();action=j.get("action").getAsString();
        daggerTime=n(j,"daggerTime");
        leftShot=n(j,"leftShot");rightShot=n(j,"rightShot");
        smokeRemaining=n(j,"smokeRemaining");danceRemaining=n(j,"danceRemaining");smokeCharges=j.get("smokeCharges").getAsInt();
        for(int i=0;i<4;i++){ranks[i]=j.getAsJsonArray("ranks").get(i).getAsInt();cooldowns[i]=j.getAsJsonArray("cooldowns").get(i).getAsDouble();}
        List<Stack> temp=new ArrayList<>();for(JsonElement e:j.getAsJsonArray("stacks")){JsonObject s=e.getAsJsonObject();temp.add(new Stack(s.get("id").getAsInt(),s.get("count").getAsInt(),s.get("sleep").getAsBoolean()));}stacks=List.copyOf(temp);
        JsonArray b=j.getAsJsonArray("bullets");bullets=new double[b.size()][7];for(int i=0;i<b.size();i++)for(int k=0;k<7;k++)bullets[i][k]=b.get(i).getAsJsonArray().get(k).getAsDouble();
        JsonArray fx=j.getAsJsonArray("fx");effects=new double[fx.size()][9];for(int i=0;i<fx.size();i++)for(int k=0;k<9;k++)effects[i][k]=fx.get(i).getAsJsonArray().get(k).getAsDouble();
    }
    private static double n(JsonObject j,String key){return j.get(key).getAsDouble();}
}
