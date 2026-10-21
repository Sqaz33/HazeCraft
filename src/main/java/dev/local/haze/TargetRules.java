package dev.local.haze;

import dev.local.haze.sim.UnitRole;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.AbstractVillager;

public final class TargetRules {
    public static UnitRole role(Entity owner,Entity e){
        return UnitRole.classify(e==owner,e instanceof AbstractVillager,e instanceof Enemy||e.getTags().stream().anyMatch(t->t.startsWith("haze_debug_")),e instanceof Mob);
    }
    public static boolean damageable(Entity owner,Entity e){return e instanceof LivingEntity living&&living.isAlive()&&role(owner,e).damageable();}
}
