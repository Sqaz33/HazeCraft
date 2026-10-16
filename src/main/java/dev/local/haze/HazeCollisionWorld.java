package dev.local.haze;

import dev.local.haze.sim.Retail;
import dev.local.haze.sim.movement.CapsuleMover;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import java.util.*;

/** Minecraft supplies geometry only; movement rules remain in the guest controller. */
public final class HazeCollisionWorld implements CapsuleMover.World {
    private final Player player;
    public HazeCollisionWorld(Player player){this.player=player;}
    @Override public List<CapsuleMover.Box> boxes(CapsuleMover.Box b){
        AABB bounds=new AABB(b.minX(),b.minY(),b.minZ(),b.maxX(),b.maxY(),b.maxZ());
        List<CapsuleMover.Box> boxes=new ArrayList<>();
        for(var shape:player.level().getBlockCollisions(player,bounds))for(AABB box:shape.toAabbs())add(boxes,box);
        for(var shape:player.level().getEntityCollisions(player,bounds))for(AABB box:shape.toAabbs())add(boxes,box);
        return boxes;
    }
    private static void add(List<CapsuleMover.Box> out,AABB b){out.add(new CapsuleMover.Box(b.minX,b.minY,b.minZ,b.maxX,b.maxY,b.maxZ));}
    public static CapsuleMover create(){
        Retail d=Retail.DATA;double unit=Retail.SOURCE_UNIT;
        return new CapsuleMover(d.model.get("m_flStepHeight").getAsDouble()*unit,d.model.get("m_flCollisionRadius").getAsDouble()*unit,
            d.model.get("m_flCollisionHeight").getAsDouble()*unit,d.field("citadel_ability_jump","m_flCollidedWallMaxDist")*unit,
            d.nativeFloat("citadel_capsule_movement_ground_box_percentage"),d.nativeFloat("sv_mover_pogofrequency"),d.nativeFloat("sv_mover_pogodampingratio"),16);
    }
}
