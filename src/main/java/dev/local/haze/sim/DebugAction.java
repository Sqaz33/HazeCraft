package dev.local.haze.sim;

/** Explicit debug operations; every server operation still requires creative + Haze. */
public enum DebugAction {
    HEALTH_PERCENT,HEALTH_VALUE,MAX_HEALTH,LOCK_HEALTH,
    NO_COOLDOWNS,CLEAR_COOLDOWNS,ABILITY_RANK,STOP_ABILITIES,
    INFINITE_AMMO,AMMO,RELOAD,
    SPAWN_TARGET,SPAWN_GROUP,SELECT_TARGET,TARGET_HEALTH,TARGET_STACKS,TARGET_SLEEP,TARGET_RESET,CLEAR_TARGETS,CHARACTER_LEVEL;
    public static DebugAction fromId(int id){return id>=0&&id<values().length?values()[id]:null;}
    public static boolean allowed(boolean creative,boolean active){return creative&&active;}
    public static int bounded(int value,int min,int max){return Math.max(min,Math.min(max,value));}
    public static int abilityIndex(int value){return bounded(value/4,0,3);}
    public static int abilityRank(int value){return bounded(value%4,0,3);}
    public static float health(int value,float max,boolean percent){
        return Math.max(1,Math.min(max,percent?max*bounded(value,1,100)/100F:value));
    }
}
