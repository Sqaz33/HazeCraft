package dev.local.haze.sim.movement;

/** Current Haze graph uses these seven clips for its charge/dash/hit branches. */
public final class MeleeVisuals {
    public static String clip(String phase,String type,double age,double strikeAge,double delay,boolean grounded){
        boolean air=type.equals("HEAVY_AIR"),light=type.equals("LIGHT");
        return switch(phase){
            case "CHARGING" -> age>delay?(grounded?"melee_start":"melee_in_air_start"):null;
            case "GROUND_DASH","SLIDE_DASH" -> "melee_dash";
            case "AIR_DASH" -> "melee_in_air_dash";
            case "ATTACKING" -> light?"melee_quick_1":air?"melee_in_air_hit":"melee_hit";
            default -> strikeAge>=0&&strikeAge<(light?.66664:.7)?light?"melee_quick_1":air?"melee_in_air_hit":"melee_hit":null;
        };
    }
}
