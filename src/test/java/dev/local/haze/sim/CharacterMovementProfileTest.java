package dev.local.haze.sim;

import com.google.gson.*;
import dev.local.haze.sim.movement.CharacterMovementProfile;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static dev.local.haze.sim.movement.CharacterMovementProfile.Role.*;
import static org.junit.jupiter.api.Assertions.*;

final class CharacterMovementProfileTest {
    @Test void currentCharacterBindingsResolveWithoutHazeSpecificAbilityNames() throws Exception {
        Path directory=Path.of(System.getProperty("haze.movement.profiles",
            "D:/DeadlockHazeLab/research/25939744/movement"));
        JsonObject profiles=JsonParser.parseString(RetailTestFixtures.read(directory.resolve("profiles.json"))).getAsJsonObject();
        JsonObject definitions=JsonParser.parseString(RetailTestFixtures.read(directory.resolve("shared-definitions.json"))).getAsJsonObject();
        int complete=0;
        boolean differentCharacterSpeed=false,differentMeleeBinding=false;
        for(var entry:profiles.entrySet()) {
            JsonObject record=entry.getValue().getAsJsonObject();
            if(!record.getAsJsonArray("unresolvedBindings").isEmpty()) {
                assertThrows(IllegalArgumentException.class,()->new CharacterMovementProfile(record,definitions));
                continue;
            }
            CharacterMovementProfile profile=new CharacterMovementProfile(record,definitions);
            assertEquals("25939744",profile.sourceBuild());
            assertEquals(entry.getKey(),profile.character());
            assertEquals(record.getAsJsonObject("startingStats").get("EMaxMoveSpeed").getAsFloat(),profile.stat("EMaxMoveSpeed"));
            assertTrue(profile.field(MELEE,"m_flHeavyAttackRequiredHoldTime")>0);
            assertFalse(profile.definition(MANTLE).getAsJsonArray("m_vecMantleTypes").isEmpty());
            assertThrows(IllegalArgumentException.class,()->profile.field(MANTLE,"m_vecMantleTypes"));
            differentCharacterSpeed|=profile.stat("EMaxMoveSpeed")!=8.2f;
            differentMeleeBinding|=!record.getAsJsonObject("movementBindings").get("melee").getAsString().equals("ability_melee_haze");
            complete++;
        }
        assertEquals(67,complete);
        assertTrue(differentCharacterSpeed);
        assertTrue(differentMeleeBinding);
        CharacterMovementProfile haze=new CharacterMovementProfile(profiles.getAsJsonObject("hero_haze"),definitions);
        assertEquals(3,haze.stat("EStamina"));
        assertEquals(10,haze.stat("EGroundDashDistanceInMeters"));
        assertEquals(.62f,haze.stat("EGroundDashDuration"));
        assertThrows(IllegalArgumentException.class,()->haze.field(JUMP,"not_a_source_field"));
    }
}
