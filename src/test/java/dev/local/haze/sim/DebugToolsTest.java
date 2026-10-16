package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DebugToolsTest {
    @Test void canDowngradeSmokeAndRestoreExactlyItsCurrentChargeCapacity(){
        Abilities a=new Abilities(Retail.DATA);
        a.debugRank(1,3);assertEquals(3,a.ranks[1]);assertEquals((int)a.p(1,"AbilityCharges"),a.smokeCharges);
        assertTrue(a.cast(1));a.debugRank(1,0);
        assertEquals(0,a.ranks[1]);assertEquals(1,a.smokeCharges);assertEquals(0,a.cooldown(1));
        assertTrue(a.cast(1));assertFalse(a.cast(1));
        a.debugRank(-1,3);a.debugRank(4,3);assertEquals(0,a.ranks[1]);
    }
    @Test void cancellingAbilitiesRemovesDelayedAndActiveEffectsWithoutAdvancingTime(){
        Abilities a=new Abilities(Retail.DATA);
        assertTrue(a.cast(0));assertTrue(a.cast(1));assertTrue(a.cast(3));
        double time=a.time;double daggerCooldown=a.cooldown(0);
        a.debugStop();assertEquals(time,a.time);assertEquals(daggerCooldown,a.cooldown(0));
        assertFalse(a.smokeActive());assertFalse(a.invisible());assertFalse(a.flurry());assertEquals(0,a.buffUntil);
        a.advance(10);assertFalse(a.consumeDagger());assertFalse(a.smokeActive());assertFalse(a.flurry());
        a.clearCooldowns();assertTrue(a.cast(0));assertTrue(a.cast(3));
    }
    @Test void resourceControlsRespectTheActualClipAndNeverKillThePlayer(){
        assertEquals(7.3,DebugAction.health(1,730,true),1e-5);
        assertEquals(365,DebugAction.health(50,730,true));assertEquals(730,DebugAction.health(101,730,true));
        assertEquals(1,DebugAction.health(-100,730,false));assertEquals(730,DebugAction.health(Integer.MAX_VALUE,730,false));
        Gun gun=new Gun(Retail.DATA);gun.ammo=0;
        var infinite=gun.advance(.1,true,false,true,false,0,0,0);
        assertFalse(infinite.isEmpty());assertEquals(0,gun.ammo);assertEquals(0,gun.reloadRemaining);
        assertTrue(gun.advance(.1,true,false,false,false,0,0,0).isEmpty());assertTrue(gun.reloadRemaining>0);
    }
    @Test void regenCanBeDisabledAndRestoredWithoutResettingMovementOrTime(){
        Motion m=new Motion(Retail.DATA,10);m.stamina=0;
        Motion.Collision ground=(delta,crouch)->new Motion.MoveResult(delta,true,false,V3.ZERO,0);
        m.setStaminaRegenMultiplier(0);m.step(.05,Controls.NONE,ground,0,-1);
        assertEquals(0,m.stamina);assertEquals(.05,m.time);
        m.setStaminaRegenMultiplier(1);m.step(.05,Controls.NONE,ground,0,-1);
        double normal=m.stamina;assertTrue(normal>0);assertEquals(.1,m.time);
        m.setStaminaRegenMultiplier(10);m.step(.05,Controls.NONE,ground,0,-1);
        assertEquals(normal*11,m.stamina,1e-8);assertEquals(.15,m.time,1e-8);
        assertThrows(IllegalArgumentException.class,()->m.setStaminaRegenMultiplier(Double.NaN));
        assertThrows(IllegalArgumentException.class,()->m.setStaminaRegenMultiplier(-1));
    }
    @Test void debugOperationsRequireCreativeHazeAndRejectUnknownCommands(){
        assertFalse(DebugAction.allowed(false,true));assertFalse(DebugAction.allowed(true,false));assertTrue(DebugAction.allowed(true,true));
        assertNull(DebugAction.fromId(-1));assertNull(DebugAction.fromId(Integer.MAX_VALUE));
        for(var op:DebugAction.values())assertEquals(op,DebugAction.fromId(op.ordinal()));
        for(int index=0;index<4;index++)for(int rank=0;rank<4;rank++){
            int encoded=index*4+rank;assertEquals(index,DebugAction.abilityIndex(encoded));assertEquals(rank,DebugAction.abilityRank(encoded));
        }
    }
}
