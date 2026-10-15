package dev.local.haze.sim;

import com.google.gson.*;
import dev.local.haze.sim.movement.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class NativeStateParityTest {
    private static JsonObject oracle() throws Exception {
        JsonObject data=JsonParser.parseString(Files.readString(Path.of(System.getProperty("haze.native.state.oracle",
            "D:/DeadlockHazeLab/research/25939744/movement/native-state-cases.json")))).getAsJsonObject();
        assertEquals("25939744",data.getAsJsonObject("source").get("build").getAsString());
        assertEquals("b7deb2d934614b2b9a4e3edf6cf7bfb954b9f1436aadb0bf07b24f07bb7d5515",
            data.getAsJsonObject("source").get("clientSha256").getAsString());
        return data;
    }
    @Test void buttonEdgesMatchOriginalIncludingShortPressesAndHighBits() throws Exception {
        int count=0;
        for(JsonElement element:oracle().getAsJsonArray("buttons")) {
            JsonObject row=element.getAsJsonObject();long mask=1L<<row.get("bit").getAsInt();
            int initial=row.get("initial").getAsInt();
            ButtonState state=new ButtonState((initial&1)!=0?mask:0,(initial&2)!=0?mask:0,(initial&4)!=0?mask:0);
            state.event(mask,row.get("down").getAsBoolean());
            assertEquals(row.get("expectedState").getAsInt(),state.encoding(mask),row.toString());
            assertEquals(row.get("expectedPressed").getAsBoolean(),(state.pressed()&mask)!=0,row.toString());
            assertEquals(row.get("expectedReleased").getAsBoolean(),(state.released()&mask)!=0,row.toString());
            assertEquals(row.get("expectedDoublePressed").getAsBoolean(),(state.doublePressedWithinCommand()&mask)!=0,row.toString());
            count++;
        }
        assertEquals(48,count);
    }
    @Test void mixedPercentagesMatchOriginalFoldBuffers() throws Exception {
        int count=0;
        for(JsonElement element:oracle().getAsJsonArray("percentages")) {
            JsonObject row=element.getAsJsonObject();PercentageStack stack=new PercentageStack();
            for(JsonElement value:row.getAsJsonArray("input"))stack.add(value.getAsFloat());
            assertEquals(row.get("positiveProduct").getAsFloat(),stack.positiveProduct(),.000001,row.toString());
            assertEquals(row.get("negativeProduct").getAsFloat(),stack.negativeProduct(),.000001,row.toString());
            count++;
        }
        assertEquals(77,count);
    }
}
