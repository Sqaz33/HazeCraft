package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DebugCooldownTest {
    @Test void refillsCooldownsAndSmokeChargesWithoutCancellingCastsOrEffects(){
        Abilities a=new Abilities(Retail.DATA);a.ranks[1]=2;
        assertTrue(a.cast(0));assertTrue(a.cast(1));
        double daggerAt=a.daggerAt,smokeStart=a.smokeStart,smokeUntil=a.smokeUntil,time=a.time;
        a.clearCooldowns();
        for(int i=0;i<4;i++)assertEquals(0,a.cooldown(i));
        assertEquals((int)a.p(1,"AbilityCharges"),a.smokeCharges);
        assertEquals(time,a.time);assertEquals(daggerAt,a.daggerAt);
        assertEquals(smokeStart,a.smokeStart);assertEquals(smokeUntil,a.smokeUntil);assertTrue(a.smokeActive());
        assertTrue(a.cast(1));a.clearCooldowns();assertTrue(a.cast(1));
    }
}
