package dev.local.haze.client;

import com.google.gson.*;
import dev.local.haze.sim.Retail;
import dev.local.haze.sim.ItemInventory;
import java.util.*;

public final class ClientState {
    public final UUID uuid;
    public final int entityId;
    public final boolean active,invisible,smoke,flurry,inCombat;
    public final boolean debugNoCooldowns;
    public final boolean debugInfiniteAmmo,debugLockHealth;
    public final JsonObject debugTarget;
    public final String meleePhase,meleeType;
    public final double meleeAge,meleeChargeAge,meleeCooldown,meleeStrikeAge;
    public final float meleeYaw;
    public final long meleeSerial,meleeStrikeSerial;
    public final List<String> items;
    public final int itemSlots,clipSize;
    public final String shopMessage;
    public final double itemMoveSpeed,itemFlight,itemFlightSpeed,itemImpulse,itemBarrier;public final long itemMovementSerial,itemStaminaSerial;public final double[] itemCooldowns=new double[4];
    public final ItemInventory itemInventory;
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
        debugInfiniteAmmo=j.has("debugInfiniteAmmo")&&j.get("debugInfiniteAmmo").getAsBoolean();
        debugLockHealth=j.has("debugLockHealth")&&j.get("debugLockHealth").getAsBoolean();
        debugTarget=j.has("debugTarget")?j.getAsJsonObject("debugTarget"):null;
        List<String> ownedItems=new ArrayList<>();if(j.has("items"))for(var id:j.getAsJsonArray("items"))ownedItems.add(id.getAsString());items=List.copyOf(ownedItems);
        itemSlots=j.has("itemSlots")?j.get("itemSlots").getAsInt():8;clipSize=j.has("clipSize")?j.get("clipSize").getAsInt():Retail.DATA.weapon().get("m_iClipSize").getAsInt();
        itemMoveSpeed=j.has("itemMoveSpeed")?n(j,"itemMoveSpeed"):0;itemFlight=j.has("itemFlight")?n(j,"itemFlight"):0;itemFlightSpeed=j.has("itemFlightSpeed")?n(j,"itemFlightSpeed"):0;itemImpulse=j.has("itemImpulse")?n(j,"itemImpulse"):0;itemBarrier=j.has("itemBarrier")?n(j,"itemBarrier"):0;itemMovementSerial=j.has("itemMovementSerial")?j.get("itemMovementSerial").getAsLong():0;itemStaminaSerial=j.has("itemStaminaSerial")?j.get("itemStaminaSerial").getAsLong():0;
        if(j.has("itemCooldowns"))for(int i=0;i<Math.min(4,j.getAsJsonArray("itemCooldowns").size());i++)itemCooldowns[i]=j.getAsJsonArray("itemCooldowns").get(i).getAsDouble();
        shopMessage=j.has("shopMessage")?j.get("shopMessage").getAsString():"";
        itemInventory=new ItemInventory();itemInventory.capacity=itemSlots;for(String id:items)itemInventory.buy(id);
        meleePhase=j.has("meleePhase")?j.get("meleePhase").getAsString():"NONE";meleeType=j.has("meleeType")?j.get("meleeType").getAsString():"NONE";
        meleeAge=j.has("meleeAge")?n(j,"meleeAge"):0;meleeChargeAge=j.has("meleeChargeAge")?n(j,"meleeChargeAge"):0;
        meleeCooldown=j.has("meleeCooldown")?n(j,"meleeCooldown"):0;meleeYaw=j.has("meleeYaw")?j.get("meleeYaw").getAsFloat():0;
        meleeStrikeAge=j.has("meleeStrikeAge")?n(j,"meleeStrikeAge"):100;
        meleeSerial=j.has("meleeSerial")?j.get("meleeSerial").getAsLong():0;meleeStrikeSerial=j.has("meleeStrikeSerial")?j.get("meleeStrikeSerial").getAsLong():0;
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
