package dev.local.haze.sim;

import com.google.gson.*;
import dev.local.haze.sim.movement.AirDrag;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeAirDragParityTest {
    private static V3 source(JsonArray a){return new V3(a.get(0).getAsFloat(),a.get(2).getAsFloat(),a.get(1).getAsFloat());}
    @Test void matches432CurrentNativeCases() throws Exception {
        JsonObject data=JsonParser.parseString(RetailTestFixtures.read(Path.of("D:/DeadlockHazeLab/research/25950029/movement/native-air-drag-cases.json"))).getAsJsonObject();
        assertEquals("fadaf78301af6d47d286de6199d1fd0aba7053f6ab597afa3de55369f72424c6",data.get("sourceSha256").getAsString());
        JsonArray cases=data.getAsJsonArray("cases");assertEquals(432,cases.size());
        for(JsonElement element:cases){
            JsonObject row=element.getAsJsonObject();JsonArray input=row.getAsJsonArray("inputs");
            V3 result=AirDrag.apply(source(row.getAsJsonArray("velocity")),row.get("yaw").getAsFloat(),
                input.get(0).getAsFloat(),input.get(1).getAsFloat(),row.get("override").getAsFloat(),
                row.get("minimumDrag").getAsFloat(),row.get("stopSpeed").getAsFloat(),row.get("slowScale").getAsFloat(),row.get("dt").getAsFloat());
            V3 expected=source(row.getAsJsonArray("output"));
            assertEquals(expected.x(),result.x(),.00015,row.toString());assertEquals(expected.y(),result.y(),0,row.toString());assertEquals(expected.z(),result.z(),.00015,row.toString());
        }
    }
}
