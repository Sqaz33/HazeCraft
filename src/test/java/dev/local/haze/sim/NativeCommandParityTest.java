package dev.local.haze.sim;

import com.google.gson.*;
import dev.local.haze.sim.movement.ButtonState;
import dev.local.haze.sim.movement.DoublePressTracker;
import java.nio.file.*;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NativeCommandParityTest {
    @Test void doublePressMatchesOriginalCommandWindowAndUnsignedWrap() throws Exception {
        Path path=Path.of(System.getProperty("haze.native.command.oracle",
            "D:/DeadlockHazeLab/research/25939744/movement/native-command-cases.json"));
        JsonObject oracle=JsonParser.parseString(RetailTestFixtures.read(path)).getAsJsonObject();
        assertEquals("25939744",oracle.getAsJsonObject("source").get("build").getAsString());
        assertEquals("b7deb2d934614b2b9a4e3edf6cf7bfb954b9f1436aadb0bf07b24f07bb7d5515",
            oracle.getAsJsonObject("source").get("clientSha256").getAsString());
        int count=0;
        for(JsonElement element:oracle.getAsJsonArray("cases")) {
            JsonObject row=element.getAsJsonObject();
            int bit=row.get("bit").getAsInt(),state=row.get("state").getAsInt();
            long mask=1L<<bit;
            int[] history=new int[64];Arrays.fill(history,-65536);
            history[bit]=(int)row.get("previous").getAsLong();
            DoublePressTracker tracker=new DoublePressTracker(history);
            long actual=tracker.update(row.get("command").getAsInt(),
                new ButtonState((state&1)!=0?mask:0,(state&2)!=0?mask:0,(state&4)!=0?mask:0),
                row.get("window").getAsFloat(),row.get("interval").getAsFloat());
            assertEquals(row.get("expectedDouble").getAsBoolean(),(actual&mask)!=0,"Double case "+count);
            assertEquals((int)row.get("expectedLastPress").getAsLong(),tracker.lastPress(bit),"Timestamp case "+count++);
            assertEquals(0,actual&~mask,"Unrelated buttons");
        }
        assertEquals(864,count);
    }
}
