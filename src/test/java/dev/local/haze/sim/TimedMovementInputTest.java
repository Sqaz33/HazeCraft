package dev.local.haze.sim;

import dev.local.haze.sim.movement.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class TimedMovementInputTest {
    @Test void directionChangesAndJumpKeepTheirOriginalOrderInsideOneHostFrame(){
        MovementKeys keys=new MovementKeys(0);keys.event(Controls.FORWARD,true,10_000_000);
        keys.event(Controls.JUMP,true,20_000_000);keys.event(Controls.JUMP,false,25_000_000);
        keys.event(Controls.FORWARD,false,30_000_000);keys.event(Controls.RIGHT,true,30_000_000);
        Controls input=keys.sample(Controls.RIGHT,0,0,0,50_000_000);MovementClock clock=new MovementClock();
        double[] time={0};List<Double> jumps=new ArrayList<>();double[] forward={0},right={0};
        clock.advance(.05,input,(dt,c)->{
            if(c.edge(Controls.JUMP))jumps.add(time[0]);if(c.down(Controls.FORWARD))forward[0]+=dt;if(c.down(Controls.RIGHT))right[0]+=dt;time[0]+=dt;
        });
        assertEquals(1,jumps.size());assertEquals(.02,jumps.getFirst(),1e-10);assertEquals(.02,forward[0],1e-10);assertEquals(.02,right[0],1e-10);assertEquals(.05,time[0],1e-10);
    }
    @Test void eventAtFrameEndAppliesOnceWithoutInventingPhysicsTime(){
        MovementKeys keys=new MovementKeys(0);keys.event(Controls.JUMP,true,50_000_000);keys.event(Controls.JUMP,false,50_000_000);
        var input=keys.sample(0,0,0,0,50_000_000);MovementClock clock=new MovementClock();int[] jumps={0};
        double[] elapsed={0};
        clock.advance(.05,input,(dt,c)->{elapsed[0]+=dt;if(c.edge(Controls.JUMP)){jumps[0]++;assertEquals(0,dt);}});assertEquals(1,jumps[0]);assertEquals(.05,elapsed[0],1e-10);
        clock.advance(.05,Controls.NONE,(dt,c)->{if(c.edge(Controls.JUMP))jumps[0]++;});assertEquals(1,jumps[0]);
        clock.advance(.05,Controls.NONE,(dt,c)->{if(c.edge(Controls.JUMP))jumps[0]++;});assertEquals(1,jumps[0]);
    }
    @Test void downwardDashWaitsForSecondCrouchPress(){
        MovementKeys keys=new MovementKeys(0);keys.sample(Controls.CROUCH,0,0,0,1);keys.sample(0,0,0,1,50_000_000);
        keys.event(Controls.CROUCH,true,80_000_000);var input=keys.sample(Controls.CROUCH,0,0,3,100_000_000);
        MovementClock clock=new MovementClock();double[] time={0},dash={-1};
        clock.advance(.05,input,(dt,c)->{if(c.edge(Controls.DOWN_DASH))dash[0]=time[0];time[0]+=dt;});
        assertEquals(.03,dash[0],1e-9);
    }
}
