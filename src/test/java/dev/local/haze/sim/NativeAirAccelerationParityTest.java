package dev.local.haze.sim;

import com.google.gson.*;
import dev.local.haze.sim.movement.AirAcceleration;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeAirAccelerationParityTest {
    private static V3 source(JsonArray a){return new V3(a.get(0).getAsFloat(),a.get(2).getAsFloat(),a.get(1).getAsFloat());}
    @Test void matches288NativeAirAccelerationCases() throws Exception {
        JsonObject data=JsonParser.parseString(Files.readString(Path.of("D:/DeadlockHazeLab/research/25950029/movement/native-air-accel-cases.json"))).getAsJsonObject();
        assertEquals("fadaf78301af6d47d286de6199d1fd0aba7053f6ab597afa3de55369f72424c6",data.get("sourceSha256").getAsString());
        JsonArray cases=data.getAsJsonArray("cases");assertEquals(288,cases.size());
        for(JsonElement element:cases){
            JsonObject row=element.getAsJsonObject();
            V3 result=AirAcceleration.apply(source(row.getAsJsonArray("velocity")),source(row.getAsJsonArray("wish")),row.get("wishSpeed").getAsFloat(),
                row.get("override").getAsFloat(),80,50,1,10,row.get("accelScale").getAsFloat(),row.get("dt").getAsFloat());
            V3 expected=source(row.getAsJsonArray("output"));
            assertEquals(expected.x(),result.x(),.0001,row.toString());assertEquals(expected.y(),result.y(),.0001,row.toString());assertEquals(expected.z(),result.z(),.0001,row.toString());
        }
    }
}
