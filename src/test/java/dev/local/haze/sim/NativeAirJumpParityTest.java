package dev.local.haze.sim;

import com.google.gson.*;
import dev.local.haze.sim.movement.JumpImpulse;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class NativeAirJumpParityTest {
    private static V3 source(JsonArray a) { return new V3(a.get(0).getAsFloat(),a.get(2).getAsFloat(),a.get(1).getAsFloat()); }
    @Test void impulseMatches120OriginalCurrentBuildCases() throws Exception {
        Path file=Path.of(System.getProperty("haze.airjump.cases","D:/DeadlockHazeLab/research/25950029/movement/native-air-jump-cases.json"));
        JsonObject data=JsonParser.parseString(Files.readString(file)).getAsJsonObject();
        assertEquals("25950029",data.get("build").getAsString());
        assertEquals("fadaf78301af6d47d286de6199d1fd0aba7053f6ab597afa3de55369f72424c6",data.get("sourceSha256").getAsString());
        JsonArray cases=data.getAsJsonArray("cases");assertEquals(120,cases.size());
        for(JsonElement element:cases) {
            JsonObject row=element.getAsJsonObject();
            V3 result=JumpImpulse.air(source(row.getAsJsonArray("velocity")),source(row.getAsJsonArray("wish")),
                row.get("maxSpeed").getAsFloat(),200,row.get("verticalSpeed").getAsFloat(),row.get("bonusPercent").getAsFloat(),1);
            V3 expected=source(row.getAsJsonArray("output"));
            assertEquals(expected.x(),result.x(),1e-5,row.toString());assertEquals(expected.y(),result.y(),1e-5,row.toString());assertEquals(expected.z(),result.z(),1e-5,row.toString());
            assertEquals(1,row.get("airJumpCount").getAsInt());
        }
        assertEquals(1.75f,JumpImpulse.bonusScale(75));
    }
}
