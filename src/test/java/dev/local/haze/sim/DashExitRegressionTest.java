package dev.local.haze.sim;

import dev.local.haze.sim.movement.MovementClock;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DashExitRegressionTest {
    private static final Motion.Collision AIR=(delta,crouch)->new Motion.MoveResult(delta,false,false,V3.ZERO,0);

    @Test void airDashKeepsMomentumAcrossPartialLastStepAndFallsAfterRelease() {
        for(double dt:new double[]{1.0/64,1.0/120,.05,.003125}) {
            Motion motion=new Motion(Retail.DATA);motion.grounded=false;
            motion.step(dt,new Controls(Controls.FORWARD|Controls.DASH,Controls.DASH,0,0),AIR,0,-1,true);
            for(int i=0;motion.mode==Motion.Mode.DASH_AIR&&i<200;i++) {
                motion.step(dt,Controls.NONE,AIR,0,-1,true);
                assertTrue(motion.velocity.horizontal()>0,"Lost velocity with dt="+dt);
            }
            assertEquals(Motion.Mode.AIR,motion.mode);
            double exitSpeed=motion.velocity.horizontal();assertTrue(exitSpeed>12,"Dash failed to build speed");
            for(int i=0;i<(int)(.2/dt);i++)motion.step(dt,Controls.NONE,AIR,0,-1,true);
            assertTrue(motion.velocity.horizontal()>exitSpeed*.8,"Post-dash momentum was lost");assertTrue(motion.velocity.y()<0,"Gravity must resume after air dash");
        }
    }
    @Test void actualHostClockDoesNotIntroduceAnExitSpeedDip() {
        Motion motion=new Motion(Retail.DATA);motion.grounded=false;MovementClock clock=new MovementClock();
        for(int frame=0;frame<12;frame++) {
            Controls input=frame==0?new Controls(Controls.FORWARD|Controls.DASH,Controls.DASH,0,0):Controls.NONE;
            clock.advance(.05,input,(dt,keys)->{
                motion.step(dt,keys,AIR,0,-1,true);
                if(motion.time>.43)assertTrue(motion.velocity.horizontal()>10,"Boundary fragment erased dash momentum");
            });
        }
        assertEquals(Motion.Mode.AIR,motion.mode);assertTrue(motion.velocity.y()<0);
    }
    @Test void groundDashOffLedgeKeepsItsExitVelocity() {
        for(double dt:new double[]{1.0/64,1.0/120,.05}) {
            Motion motion=new Motion(Retail.DATA);
            motion.step(dt,new Controls(Controls.FORWARD|Controls.DASH,Controls.DASH,0,0),AIR,0,-1,true);
            for(int i=0;motion.mode==Motion.Mode.DASH_GROUND&&i<100;i++) {
                motion.step(dt,Controls.NONE,AIR,0,-1,true);
                assertTrue(motion.velocity.horizontal()>15,"Ground dash fragment erased velocity");
            }
            assertEquals(Motion.Mode.AIR,motion.mode);assertTrue(motion.velocity.y()<0);
        }
    }
    @Test void requestedFastRegenRefillsOneChargeInPoint45SecondsAndClampsAtMaximum() {
        Motion motion=new Motion(Retail.DATA,10);motion.stamina=0;
        for(int i=0;i<9;i++)motion.step(.05,Controls.NONE,AIR,0,-1,true);
        assertEquals(1,motion.stamina,1e-6);
        for(int i=0;i<30;i++)motion.step(.05,Controls.NONE,AIR,0,-1,true);
        assertEquals(motion.maxStamina(),motion.stamina);
    }
}
