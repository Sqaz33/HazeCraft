package dev.local.haze.sim;

import com.google.gson.*;
import dev.local.haze.sim.movement.DashParryGravity;
import dev.local.haze.sim.movement.SupportSpring;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NativeSupportParityTest {
    private static JsonObject oracle() throws Exception {
        Path path=Path.of(System.getProperty("haze.native.support.oracle",
            "D:/DeadlockHazeLab/research/25939744/movement/native-support-cases.json"));
        JsonObject result=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals("25939744",result.getAsJsonObject("source").get("build").getAsString());
        assertEquals("b7deb2d934614b2b9a4e3edf6cf7bfb954b9f1436aadb0bf07b24f07bb7d5515",
            result.getAsJsonObject("source").get("clientSha256").getAsString());
        return result;
    }
    private static float number(JsonObject row,String key) { return row.get(key).getAsFloat(); }
    private static V3 vector(JsonArray row) {
        return new V3(row.get(0).getAsFloat(),row.get(2).getAsFloat(),row.get(1).getAsFloat());
    }
    private static void assertVector(V3 expected,V3 actual,int index) {
        assertEquals(expected.x(),actual.x(),.0001,"X case "+index);
        assertEquals(expected.y(),actual.y(),.0001,"Up case "+index);
        assertEquals(expected.z(),actual.z(),.0001,"Z case "+index);
    }
    @Test void supportSpringMatchesNativeGroundAndFallingTransitions() throws Exception {
        int count=0;
        for(JsonElement element:oracle().getAsJsonArray("spring")) {
            JsonObject row=element.getAsJsonObject(),in=row.getAsJsonObject("input");
            SupportSpring.Result actual=SupportSpring.integrate(vector(in.getAsJsonArray("springVelocity")),
                vector(in.getAsJsonArray("velocity")),vector(in.getAsJsonArray("support")),number(in,"originUp"),
                in.get("grounded").getAsBoolean(),in.get("mode").getAsInt(),in.get("disabled").getAsBoolean(),
                number(in,"frequency"),number(in,"damping"),160000,number(in,"dt"));
            assertVector(vector(row.getAsJsonArray("springVelocity")),actual.springVelocity(),count);
            assertVector(vector(row.getAsJsonArray("velocity")),actual.velocity(),count++);
        }
        assertEquals(144,count);
    }
    @Test void dashParryGravityMatchesNativeLifetimeAndRecovery() throws Exception {
        int count=0;
        for(JsonElement element:oracle().getAsJsonArray("parry")) {
            JsonObject row=element.getAsJsonObject(),in=row.getAsJsonObject("input");
            DashParryGravity actual=DashParryGravity.at(number(in,"start"),number(in,"time"),
                number(in,"duration"),number(in,"initialScale"));
            assertEquals(row.get("active").getAsBoolean(),actual.active(),"Presence case "+count);
            assertEquals(number(row,"scale"),actual.scale(),.000001,"Scale case "+count++);
        }
        assertEquals(120,count);
    }
}
