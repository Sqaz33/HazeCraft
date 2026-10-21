package dev.local.haze.sim;

import com.google.gson.*;
import dev.local.haze.sim.movement.MantleTrajectory;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NativeMantleParityTest {
    private static V3 vector(JsonArray a) {
        return new V3(a.get(0).getAsFloat(),a.get(2).getAsFloat(),a.get(1).getAsFloat());
    }
    @Test void trajectoryMatchesOriginalX64AcrossHeightsHeadingsAndTickIntervals() throws Exception {
        Path path=Path.of(System.getProperty("haze.native.mantle.oracle",
            "D:/DeadlockHazeLab/research/25939744/movement/native-mantle-cases.json"));
        JsonObject oracle=JsonParser.parseString(RetailTestFixtures.read(path)).getAsJsonObject();
        assertEquals("25939744",oracle.getAsJsonObject("source").get("build").getAsString());
        assertEquals("b7deb2d934614b2b9a4e3edf6cf7bfb954b9f1436aadb0bf07b24f07bb7d5515",
            oracle.getAsJsonObject("source").get("clientSha256").getAsString());
        int count=0;
        for(JsonElement element:oracle.getAsJsonArray("cases")) {
            JsonObject row=element.getAsJsonObject(),in=row.getAsJsonObject("input");
            MantleTrajectory trajectory=new MantleTrajectory(vector(in.getAsJsonArray("start")),
                vector(in.getAsJsonArray("end")),in.get("startTime").getAsFloat(),
                in.get("verticalTime").getAsFloat(),in.get("horizontalTime").getAsFloat(),
                in.get("tickInterval").getAsFloat(),in.get("frontCorrection").getAsFloat(),
                in.get("upCorrection").getAsFloat());
            V3 actual=trajectory.sample(in.get("time").getAsFloat()),expected=vector(row.getAsJsonArray("expected"));
            assertEquals(expected.x(),actual.x(),.0001,"X, native case "+count);
            assertEquals(expected.y(),actual.y(),.0001,"Up, native case "+count);
            assertEquals(expected.z(),actual.z(),.0001,"Z, native case "+count);
            count++;
        }
        assertEquals(396,count);
    }
}
