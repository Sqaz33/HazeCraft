package dev.local.haze.sim;

/** Minecraft-to-Deadlock roles explicitly requested by the owner. */
public enum UnitRole {
    SELF, ENEMY_HERO, ALLY_HERO, CREEP, OTHER;
    public boolean damageable(){return this==ENEMY_HERO||this==CREEP;}
    public boolean hero(){return this==ENEMY_HERO||this==ALLY_HERO||this==SELF;}
    public boolean friendly(){return this==ALLY_HERO||this==SELF;}
    public static UnitRole classify(boolean self,boolean villager,boolean hostile,boolean mob){
        if(self)return SELF;if(villager)return ALLY_HERO;if(hostile)return ENEMY_HERO;return mob?CREEP:OTHER;
    }
}
