package dev.local.haze.sim;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** Independent original-x64 results; the port never generates its expected values. */
final class NativeMathParityTest {
    @Test void pureKernelsMatchCurrentOwnedClient() throws Exception {
        Path path=Path.of(System.getProperty("haze.native.oracle",
            "D:/DeadlockHazeLab/research/25939744/movement/native-math-cases.json"));
        JsonObject oracle=JsonParser.parseString(Files.readString(path)).getAsJsonObject();
        assertEquals("25939744",oracle.getAsJsonObject("source").get("build").getAsString());
        assertEquals("b7deb2d934614b2b9a4e3edf6cf7bfb954b9f1436aadb0bf07b24f07bb7d5515",
            oracle.getAsJsonObject("source").get("clientSha256").getAsString());
        int count=0;
        for(JsonElement element:oracle.getAsJsonArray("cases")){
            JsonObject row=element.getAsJsonObject();JsonArray in=row.getAsJsonArray("input");
            String kernel=row.get("kernel").getAsString();double actual;
            switch(kernel){
                case "bias" -> actual=SourceMath.bias(in.get(0).getAsFloat(),in.get(1).getAsFloat());
                case "remap" -> actual=SourceMath.remapClamped(in.get(0).getAsFloat(),in.get(1).getAsFloat(),
                    in.get(2).getAsFloat(),in.get(3).getAsFloat(),in.get(4).getAsFloat());
                case "curve" -> actual=Curve.sample(oracle.getAsJsonObject("curves").getAsJsonObject(row.get("curve").getAsString()),in.get(0).getAsFloat());
                case "airControlBlend" -> actual=SourceMath.remapClamped(in.get(0).getAsFloat(),600,800,0,1);
                default -> throw new AssertionError("Unknown native kernel: "+kernel);
            }
            assertEquals(row.get("expected").getAsDouble(),actual,kernel.equals("curve")?0.00005:0.000001,
                "Native case "+count+": "+row);
            count++;
        }
        assertEquals(198,count);
    }
}
