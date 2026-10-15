package dev.local.haze.sim.movement;

import com.google.gson.*;
import java.util.*;

/** Character data selected by role. No hero IDs, Minecraft types or global Retail table. */
public final class CharacterMovementProfile {
    public enum Role { JUMP, DASH, SLIDE, SPRINT, MANTLE, PARRY, MELEE }
    private final String character,sourceBuild;
    private final JsonObject stats;
    private final JsonElement groundDashCurve;
    private final EnumMap<Role,JsonObject> abilities=new EnumMap<>(Role.class);

    public CharacterMovementProfile(JsonObject characterRecord,JsonObject sharedDefinitions) {
        if(!characterRecord.getAsJsonArray("unresolvedBindings").isEmpty())
            throw new IllegalArgumentException("Unresolved source movement bindings");
        character=characterRecord.get("sourceHero").getAsString();
        sourceBuild=characterRecord.get("sourceBuild").getAsString();
        stats=characterRecord.getAsJsonObject("startingStats").deepCopy();
        groundDashCurve=characterRecord.get("groundDashPositionCurve").deepCopy();
        JsonObject bindings=characterRecord.getAsJsonObject("movementBindings");
        for(Role role:Role.values()) {
            JsonElement binding=bindings.get(role.name().toLowerCase(Locale.ROOT));
            if(binding==null || !sharedDefinitions.has(binding.getAsString()))
                throw new IllegalArgumentException("Missing movement role: "+role);
            abilities.put(role,sharedDefinitions.getAsJsonObject(binding.getAsString()).deepCopy());
        }
    }
    public String character() { return character; }
    public String sourceBuild() { return sourceBuild; }
    public float stat(String key) { return number(stats.get(key),key); }
    public float field(Role role,String key) { return number(abilities.get(role).get(key),role+"/"+key); }
    public float baseProperty(Role role,String key) {
        JsonObject property=abilities.get(role).getAsJsonObject("m_mapAbilityProperties").getAsJsonObject(key);
        if(property==null)throw new IllegalArgumentException("Missing source property: "+role+"/"+key);
        return number(property.get("m_strValue"),role+"/"+key);
    }
    public JsonObject definition(Role role) { return abilities.get(role).deepCopy(); }
    public JsonElement groundDashCurve() { return groundDashCurve.deepCopy(); }
    private static float number(JsonElement element,String key) {
        if(element==null || !element.isJsonPrimitive())
            throw new IllegalArgumentException("Missing source number: "+key);
        String text=element.getAsString();
        if(text.endsWith("m"))text=text.substring(0,text.length()-1);
        float value=Float.parseFloat(text);
        if(!Float.isFinite(value))throw new IllegalArgumentException("Invalid source number: "+key);
        return value;
    }
}
