package dev.local.haze.sim;

import dev.local.haze.sim.movement.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JumpSlideRegressionTest {
    private static Motion.Collision floor(V3[] position) {
        return (delta,crouch)->{
            boolean ground=position[0].y()+delta.y()<=0;
            V3 actual=ground?delta.withY(-position[0].y()):delta;
            position[0]=position[0].add(actual);return new Motion.MoveResult(actual,ground,false,V3.ZERO,0);
        };
    }
    @Test void twoSpaceTapsInsideOneHostFrameProduceGroundAndAirJump() {
        for(boolean heldAtPoll:new boolean[]{true,false}) {
            MovementKeys keys=new MovementKeys();keys.event(Controls.JUMP,true);keys.event(Controls.JUMP,false);
            keys.event(Controls.JUMP,true);if(!heldAtPoll)keys.event(Controls.JUMP,false);
            Controls input=keys.sample(heldAtPoll?Controls.JUMP:0,0,0,100);
            assertEquals(2,input.jumpPresses());
            Motion motion=new Motion(Retail.DATA,10);V3[] position={V3.ZERO};
            new MovementClock().advance(.05,input,(dt,buttons)->motion.step(dt,buttons,floor(position),0,-1,true));
            assertTrue(motion.airJumpUsed,"Second tap was swallowed");assertTrue(motion.velocity.y()>4);
            assertTrue(position[0].y()>0);assertTrue(motion.stamina<2.2);
            assertEquals(0,keys.sample(heldAtPoll?Controls.JUMP:0,0,0,104).jumpPresses());
        }
    }
    @Test void separatedSecondPressWorksAtDifferentAirborneTimes() {
        for(int delay:new int[]{1,2,4,8,12}) {
            Motion motion=new Motion(Retail.DATA,10);MovementKeys keys=new MovementKeys();MovementClock clock=new MovementClock();
            V3[] position={V3.ZERO};var world=floor(position);
            clock.advance(.05,keys.sample(Controls.JUMP,0,0,0),(dt,input)->motion.step(dt,input,world,0,-1,true));
            for(int tick=0;tick<delay;tick++)clock.advance(.05,keys.sample(0,0,0,tick*3+3),(dt,input)->motion.step(dt,input,world,0,-1,true));
            clock.advance(.05,keys.sample(Controls.JUMP,0,0,delay*3+6),(dt,input)->motion.step(dt,input,world,0,-1,true));
            assertTrue(motion.airJumpUsed,"Air jump failed after "+delay+" host frames");assertTrue(motion.velocity.y()>4);
        }
    }
    @Test void holdingOrDuplicateDownEventDoesNotCauseAutomaticDoubleJump() {
        MovementKeys keys=new MovementKeys();keys.event(Controls.JUMP,true);keys.event(Controls.JUMP,true);
        assertEquals(1,keys.sample(Controls.JUMP,0,0,0).jumpPresses());
        assertEquals(0,keys.sample(Controls.JUMP,0,0,3).jumpPresses());
    }
    @Test void queuedJumpSurvivesShortHostFrameWithoutRepeatingOtherActions() {
        MovementClock clock=new MovementClock();int[] jumps={0},dash={0};
        clock.advance(.001,new Controls(0,Controls.JUMP|Controls.DASH,0,0,2),(dt,input)->{
            if(input.edge(Controls.JUMP))jumps[0]++;if(input.edge(Controls.DASH))dash[0]++;
        });
        clock.advance(.05,Controls.NONE,(dt,input)->{
            if(input.edge(Controls.JUMP))jumps[0]++;if(input.edge(Controls.DASH))dash[0]++;
        });
        assertEquals(2,jumps[0]);assertEquals(1,dash[0]);
    }
    @Test void slidingFiresFromEmptyMagazineAndInterruptsReloadWithoutAddingAmmo() {
        Gun gun=new Gun(Retail.DATA);gun.ammo=0;gun.reload(0);assertTrue(gun.reloadRemaining>0);
        long before=gun.shotCount;
        for(int tick=0;tick<40;tick++)gun.advance(.05,true,false,false,true,0,0,0);
        assertTrue(gun.shotCount-before>=19);assertEquals(0,gun.ammo);assertEquals(0,gun.reloadRemaining);
        for(int i=0;i<4;i++)gun.advance(.05,true,false,false,false,0,0,0);
        assertTrue(gun.reloadRemaining>0,"Leaving slide must restore normal ammo rules");
    }
    @Test void slideDoesNotSpendLastRoundAndNormalFireResumesSpendingIt() {
        Gun gun=new Gun(Retail.DATA);gun.ammo=1;
        for(int i=0;i<40;i++)gun.advance(.05,true,false,false,true,0,0,0);
        assertEquals(1,gun.ammo);
        for(int i=0;i<4;i++)gun.advance(.05,true,false,false,false,0,0,0);
        assertEquals(0,gun.ammo);
    }
}
