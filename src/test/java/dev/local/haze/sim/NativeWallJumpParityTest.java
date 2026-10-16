package dev.local.haze.sim;

import com.google.gson.*;
import dev.local.haze.sim.movement.WallJumpImpulse;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeWallJumpParityTest {
    private static V3 source(JsonArray a){return new V3(a.get(0).getAsFloat(),a.get(2).getAsFloat(),a.get(1).getAsFloat());}
    @Test void matches200NativeImpulsesAndLateralCurveArguments() throws Exception {
        JsonObject data=JsonParser.parseString(Files.readString(Path.of("D:/DeadlockHazeLab/research/25950029/movement/native-wall-jump-cases.json"))).getAsJsonObject();
        assertEquals("fadaf78301af6d47d286de6199d1fd0aba7053f6ab597afa3de55369f72424c6",data.get("sourceSha256").getAsString());
        JsonArray cases=data.getAsJsonArray("cases");assertEquals(200,cases.size());
        for(JsonElement element:cases){
            JsonObject row=element.getAsJsonObject();
            V3 result=WallJumpImpulse.apply(source(row.getAsJsonArray("velocity")),source(row.getAsJsonArray("wish")),source(row.getAsJsonArray("normal")),
                .6f,225,310,525,100,row.get("power").getAsFloat(),input->{assertEquals(row.get("curveInput").getAsFloat(),input,.00015,row.toString());return row.get("falloff").getAsFloat();});
            V3 expected=source(row.getAsJsonArray("output"));
            assertEquals(expected.x(),result.x(),.00015,row.toString());assertEquals(expected.y(),result.y(),.00015,row.toString());assertEquals(expected.z(),result.z(),.00015,row.toString());
        }
    }
}
