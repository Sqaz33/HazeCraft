package dev.local.haze.sim;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeAirDashTrajectoryTest {
    private static final Motion.Collision AIR=(delta,crouch)->new Motion.MoveResult(delta,false,false,V3.ZERO,0);
    private static V3 source(JsonArray a){return new V3(a.get(0).getAsDouble(),a.get(2).getAsDouble(),a.get(1).getAsDouble());}
    @Test void rampReleaseAndCoastMatchComposedNativeKernels() throws Exception {
        JsonObject data=JsonParser.parseString(RetailTestFixtures.read(Path.of("D:/DeadlockHazeLab/research/25950029/movement/native-air-dash-traces.json"))).getAsJsonObject();
        assertEquals("fadaf78301af6d47d286de6199d1fd0aba7053f6ab597afa3de55369f72424c6",data.get("sourceSha256").getAsString());
        assertEquals(12,data.getAsJsonArray("traces").size());
        for(JsonElement item:data.getAsJsonArray("traces")){
            JsonObject trace=item.getAsJsonObject();double dt=trace.get("dt").getAsDouble();Motion motion=new Motion(Retail.DATA);motion.grounded=false;
            motion.velocity=new V3(0,0,trace.get("initialSpeed").getAsDouble());V3[] position={V3.ZERO};int index=0;
            Motion.Collision world=(delta,crouch)->{position[0]=position[0].add(delta);return AIR.move(delta,crouch);};
            for(JsonElement frame:trace.getAsJsonArray("frames")){
                JsonObject row=frame.getAsJsonObject();motion.step(dt,index++==0?new Controls(Controls.FORWARD|Controls.DASH,Controls.DASH,0,0):Controls.NONE,world,0,-1,true);
                V3 expected=source(row.getAsJsonArray("velocity"));
                assertEquals(expected.x(),motion.velocity.x(),.0002,row.toString());assertEquals(expected.y(),motion.velocity.y(),.0002,row.toString());assertEquals(expected.z(),motion.velocity.z(),.0002,row.toString());
                assertEquals(source(row.getAsJsonArray("position")).z(),position[0].z(),.0002,row.toString());
                assertEquals(row.get("active").getAsBoolean(),motion.mode==Motion.Mode.DASH_AIR,row.toString());
            }
        }
    }
}
