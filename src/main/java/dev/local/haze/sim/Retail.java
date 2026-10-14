package dev.local.haze.sim;

import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Immutable source table from the owner's pinned retail install. No wiki defaults. */
public final class Retail {
    public static final Retail DATA = load();
    public static final double SOURCE_UNIT = 0.0254;
    public static final String[] SIGNATURES = {"ability_sleep_dagger", "ability_smoke_bomb", "ability_stacking_damage", "ability_bullet_flurry"};
    public final JsonObject root;
    public final JsonObject hero;
    public final JsonObject camera;
    public final JsonObject model;
    private Retail(JsonObject root) {
        this.root = root;
        hero = root.getAsJsonObject("hero");
        camera = root.getAsJsonObject("modelKeys").getAsJsonObject("CitadelCameraSettings_t");
        model = root.getAsJsonObject("modelKeys").getAsJsonObject("CCitadelHeroModelGameData_t");
    }
    private static Retail load() {
        try (InputStream in = Retail.class.getResourceAsStream("/haze/retail/profile.json")) {
            if (in == null) throw new IllegalStateException("Run tools/prepare_retail.py against your owned Deadlock install before building");
            return new Retail(JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject());
        } catch (IOException e) { throw new UncheckedIOException(e); }
    }
    public JsonObject ability(String id) { return root.getAsJsonObject("abilities").getAsJsonObject(id); }
    public JsonObject weapon() { return ability("citadel_weapon_haze_set").getAsJsonObject("m_mapWeaponInfos").getAsJsonObject("primary"); }
    public double stat(String id) { return number(hero.getAsJsonObject("m_mapStartingStats").get(id)); }
    public double nativeFloat(String id) { return number(root.getAsJsonObject("nativeFloatDefaults").getAsJsonObject(id).get("value")); }
    public double field(String id, String key) { return number(ability(id).get(key)); }
    public double prop(String id, String key, int rank) { return prop(id,key,rank,0,0); }
    public double prop(String id, String key, int rank, double spirit, double baseWeaponIncrease) {
        JsonObject ability = ability(id);
        JsonObject p = ability.getAsJsonObject("m_mapAbilityProperties").getAsJsonObject(key);
        if (p == null) throw new IllegalArgumentException("Unrecovered retail property: " + id + "/" + key);
        double result = number(p.get("m_strValue"));
        double scale = 0;
        if (p.has("m_subclassScaleFunction")) {
            JsonObject f = p.getAsJsonObject("m_subclassScaleFunction");
            if (f.has("m_flStatScale") && f.has("m_eSpecificStatScaleType")) {
                String type = f.get("m_eSpecificStatScaleType").getAsString();
                if (type.equals("ETechPower")) scale = number(f.get("m_flStatScale"))*spirit;
                if (type.equals("EBaseWeaponDamageIncrease")) scale = number(f.get("m_flStatScale"))*baseWeaponIncrease;
            }
        }
        if (ability.has("m_vecAbilityUpgrades")) {
            JsonArray upgrades = ability.getAsJsonArray("m_vecAbilityUpgrades");
            for (int i=0;i<Math.min(rank,upgrades.size());i++) {
                JsonObject u = upgrades.get(i).getAsJsonObject();
                if (!u.has("m_vecPropertyUpgrades")) continue;
                for (JsonElement element : u.getAsJsonArray("m_vecPropertyUpgrades")) {
                    JsonObject change = element.getAsJsonObject();
                    if (!key.equals(change.get("m_strPropertyName").getAsString())) continue;
                    double bonus = number(change.get("m_strBonus"));
                    String type = change.has("m_eUpgradeType") ? change.get("m_eUpgradeType").getAsString() : "EAddToBase";
                    if (type.equals("EAddToScale")) {
                        String stat = change.has("m_eScaleStatFilter") ? change.get("m_eScaleStatFilter").getAsString() : "ETechPower";
                        scale += bonus*(stat.equals("EBaseWeaponDamageIncrease") ? baseWeaponIncrease : spirit);
                    } else if (type.equals("EMultiply")) result *= bonus;
                    else result += bonus;
                }
            }
        }
        return result + scale;
    }
    public static double number(JsonElement value) {
        if (value == null) throw new IllegalArgumentException("Missing retail numeric value");
        String s = value.getAsString();
        if (s.endsWith("m")) s = s.substring(0,s.length()-1);
        return Double.parseDouble(s);
    }
}
