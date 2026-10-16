package dev.local.haze.sim;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class ItemCatalog {
    public record Item(String id,String name,String icon,String description,int tier,int price,String category,boolean active,List<String> components,Map<String,Double> stats,JsonObject data){
        public double property(String key){var p=data.getAsJsonObject("m_mapAbilityProperties").getAsJsonObject(key);if(p==null||!p.has("m_strValue"))return 0;try{return Retail.number(p.get("m_strValue"));}catch(Exception e){return 0;}}
    }
    public static final Map<String,Item> ITEMS=load();
    public static final Set<String> APPLIED=Set.of("BonusHealth","BonusBaseHealth","MaxHealthLossPercent","BonusHealthRegen","OutOfCombatHealthRegen","BaseAttackDamagePercent","BonusFireRate","BonusClipSizePercent","BonusClipSize","ReloadSpeedMultipler","BonusAttackRangePercent","SpiritPower","TechPower","CooldownReduction","BonusAbilityDurationPercent","TechRangeMultiplier","BonusBulletSpeedPercent","NonPlayerBonusWeaponPower","BulletLifestealPercent","BonusMeleeDamagePercent","BonusMoveSpeed","BonusSprintSpeed","Stamina","StaminaCooldownReduction","BulletResist","MeleeResistPercent");
    public static boolean pending(Item item){
        if(item.active()||!item.data().get("_class").getAsString().equals("citadel_item")||!APPLIED.containsAll(item.stats().keySet()))return true;
        for(var modifier:item.data().getAsJsonArray("m_AutoIntrinsicModifiers"))if(!modifier.getAsJsonObject().get("_class").getAsString().equals("modifier_intrinsic_base"))return true;
        return false;
    }
    public static String label(String key){return switch(key){
        case "BonusHealth"->"Здоровье";case "BonusHealthRegen"->"Регенерация здоровья";case "OutOfCombatHealthRegen"->"Регенерация вне боя";
        case "BaseAttackDamagePercent"->"Урон оружия, %";case "BonusFireRate"->"Скорострельность, %";case "BonusClipSizePercent"->"Магазин, %";case "BonusClipSize"->"Патроны";
        case "BulletResist"->"Сопротивление пулям, %";case "TechResist"->"Сопротивление духу, %";case "MeleeResistPercent"->"Сопротивление мили, %";
        case "SpiritPower","TechPower"->"Сила духа";case "BonusMoveSpeed"->"Скорость движения";case "BonusSprintSpeed"->"Скорость спринта";case "Stamina"->"Стамина";
        case "CooldownReduction"->"Сокращение КД, %";case "BonusAbilityDurationPercent"->"Длительность умений, %";case "BulletLifestealPercent"->"Вампиризм пуль, %";
        default->key.replaceAll("([a-z])([A-Z])","$1 $2");};}
    private static Map<String,Item> load(){
        try(var in=ItemCatalog.class.getResourceAsStream("/haze/retail/items.json")){
            if(in==null)throw new IllegalStateException("Missing private item catalogue");
            var json=JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();
            Map<String,Item> out=new LinkedHashMap<>();
            for(var element:json.getAsJsonArray("items")){
                var a=element.getAsJsonObject();List<String> components=new ArrayList<>();for(var c:a.getAsJsonArray("components"))components.add(c.getAsString());
                Map<String,Double> stats=new HashMap<>();a.getAsJsonObject("stats").entrySet().forEach(e->stats.put(e.getKey(),e.getValue().getAsDouble()));
                var item=new Item(a.get("id").getAsString(),a.get("name").getAsString(),a.get("icon").getAsString(),a.get("description").getAsString(),a.get("tier").getAsInt(),a.get("price").getAsInt(),a.get("category").getAsString(),a.get("active").getAsBoolean(),List.copyOf(components),Map.copyOf(stats),a.getAsJsonObject("data"));out.put(item.id,item);
            }return Collections.unmodifiableMap(out);
        }catch(IOException e){throw new UncheckedIOException(e);}
    }
}
