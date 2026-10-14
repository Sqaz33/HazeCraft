package dev.local.haze.client;

import dev.local.haze.HazeActor;
import dev.local.haze.sim.*;
import net.minecraft.world.entity.player.Player;
import java.util.*;

/** Host geometry adapter. Lines are provisioned by the private lab/commands. */
public final class HazeZiplines {
    public record Line(V3 a,V3 b) {}
    public static final List<Line> LINES=new ArrayList<>();
    public static V3 nearDirection(Player p){
        V3 position=HazeActor.of(p.position()),look=V3.look(p.getYRot(),0);
        double best=Retail.DATA.prop("citadel_ability_zip_line","MaxMountDistance2D",0);V3 result=null;
        for(Line line:LINES){
            V3 segment=line.b.sub(line.a);double t=Math.max(0,Math.min(1,position.sub(line.a).dot(segment)/segment.dot(segment)));
            V3 nearest=line.a.add(segment.mul(t));double distance=nearest.sub(position).length();
            if(distance<best){best=distance;V3 direction=segment.normalized();result=direction.dot(look)<0?direction.mul(-1):direction;}
        }return result;
    }
}
