package dev.local.haze.sim;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** The same time-tiered, weighted deck used by the retail permanent pickup manager. */
public final class PermanentPickups {
    private static final JsonObject DATA=load();
    private final List<String> deck=new ArrayList<>();
    private int tier=-1;
    private static JsonObject load(){
        try(var in=PermanentPickups.class.getResourceAsStream("/haze/retail/pickups.json")){
            if(in==null)throw new IllegalStateException("Missing owned-game permanent pickups; run prepare_pickups.py");
            return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
    public String draw(double matchSeconds,Random random){
        var loot=DATA.getAsJsonObject("loot");var tables=loot.getAsJsonObject("m_mapPickupsByMatchTimeMins");
        int next=tables.keySet().stream().mapToInt(Integer::parseInt).filter(m->m<=matchSeconds/60).max().orElse(0);
        if(next!=tier){deck.clear();tier=next;}
        if(deck.isEmpty()){
            int size=loot.get("m_iLootListDeckSize").getAsInt();
            for(var entry:tables.getAsJsonObject(Integer.toString(tier)).entrySet()){
                // Native CVTSS2SI uses round-to-nearest-even, with at least one card for positive weights.
                double weight=entry.getValue().getAsDouble();int count=weight>0?Math.max(1,(int)Math.rint(size*weight)):0;
                for(int n=0;n<count;n++)deck.add(entry.getKey());
            }
            Collections.shuffle(deck,random);
        }
        return deck.removeLast();
    }
    public static Map<String,Double> stats(String id){
        var values=DATA.getAsJsonObject("pickups").getAsJsonArray(id);
        if(values==null)throw new IllegalArgumentException("Unknown permanent pickup: "+id);
        Map<String,Double> out=new HashMap<>();
        for(var value:values){var v=value.getAsJsonObject();String key=switch(v.get("m_eModifierValue").getAsString()){
            case "MODIFIER_VALUE_TECH_POWER"->"TechPower";
            case "MODIFIER_VALUE_FIRE_RATE"->"BonusFireRate";
            case "MODIFIER_VALUE_AMMO_CLIP_SIZE_PERCENT"->"BonusClipSizePercent";
            case "MODIFIER_VALUE_HEALTH_MAX"->"BonusHealth";
            case "MODIFIER_VALUE_COOLDOWN_REDUCTION_PERCENTAGE"->"CooldownReduction";
            case "MODIFIER_VALUE_WEAPON_DAMAGE_INCREASE"->"BaseAttackDamagePercent";
            case "MODIFIER_VALUE_BULLET_ARMOR_DAMAGE_RESIST"->"BulletResist";
            case "MODIFIER_VALUE_TECH_RESIST"->"TechResist";
            case "MODIFIER_VALUE_TECH_RANGE_PERCENT"->"TechRangeMultiplier";
            case "MODIFIER_VALUE_TECH_RADIUS_PERCENT"->"TechRadiusMultiplier";
            case "MODIFIER_VALUE_MOVEMENT_SPEED_MAX"->"BonusMoveSpeed";
            default->throw new IllegalStateException("Unmapped permanent stat: "+v);
        };double n=v.get("m_value").getAsDouble();if(key.equals("BonusMoveSpeed"))n*=Retail.SOURCE_UNIT;out.merge(key,n,Double::sum);}
        return Map.copyOf(out);
    }
}
