package dev.local.haze.sim;

import dev.local.haze.sim.movement.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementPreviewTest {
    private static final double DT=1.0/64;
    private static Motion.Collision air=(delta,crouch)->new Motion.MoveResult(delta,false,false,V3.ZERO,0);
    private static Motion.Collision floor(V3[] pos) {
        return (delta,crouch)->{
            boolean ground=pos[0].y()+delta.y()<=0;
            V3 actual=ground?delta.withY(-pos[0].y()):delta;
            pos[0]=pos[0].add(actual);return new Motion.MoveResult(actual,ground,false,V3.ZERO,0);
        };
    }
    @Test void hostClockPreservesTimePhaseAndOnlyOneActionEdge() {
        MovementClock clock=new MovementClock();double[] elapsed={0};int[] edges={0},steps={0};
        for(int tick=0;tick<1000;tick++)clock.advance(.05,new Controls(Controls.JUMP,Controls.JUMP,0,0),(dt,input)->{
            assertTrue(dt>0&&dt<=DT);elapsed[0]+=dt;steps[0]++;if(input.edge(Controls.JUMP))edges[0]++;
        });
        assertEquals(50,elapsed[0],1e-9);assertEquals(0,clock.phase(),1e-10);assertEquals(1000,edges[0]);
        assertEquals(4000,steps[0]); // 3/4 complete commands plus host-boundary fractions.
    }
    @Test void shortSpaceTapSurvivesHostPollAndDoesNotRepeat() {
        MovementKeys keys=new MovementKeys();keys.event(Controls.JUMP,true);keys.event(Controls.JUMP,false);
        Controls input=keys.sample(0,0,0,100);
        assertTrue(input.edge(Controls.JUMP));assertFalse(input.down(Controls.JUMP));
        assertFalse(keys.sample(0,0,0,103).edge(Controls.JUMP));
    }
    @Test void doubleCrouchTriggersSeparateDownwardDashWithStrictWindow() {
        MovementKeys keys=new MovementKeys();
        assertFalse(keys.sample(Controls.CROUCH,0,0,100).edge(Controls.DOWN_DASH));
        keys.sample(0,0,0,101);
        assertTrue(keys.sample(Controls.CROUCH,0,0,113).edge(Controls.DOWN_DASH));
        keys.sample(0,0,0,114);
        assertFalse(keys.sample(Controls.CROUCH,0,0,127).edge(Controls.DOWN_DASH));
        Motion motion=new Motion(Retail.DATA);motion.grounded=false;
        motion.step(DT,new Controls(Controls.CROUCH,Controls.DOWN_DASH,0,0),air,0,-1,true);
        assertTrue(motion.downDashUsed);assertFalse(motion.airDashUsed);assertEquals(2.5,motion.stamina,1e-8);
        assertEquals(-500*.0254-800*.0254*DT,motion.velocity.y(),1e-8);
    }
    @Test void threeStaminaChargesShowIndependentFullPartialAndEmptySegments() {
        var charges=StaminaDisplay.charges(1.25,3);
        assertEquals(3,charges.length);assertEquals(1,charges[0].fill());assertEquals(.25,charges[1].fill());assertEquals(0,charges[2].fill());
        assertEquals(135,charges[0].start());assertEquals(166,charges[1].start());assertEquals(197,charges[2].start());assertEquals(-28,charges[0].extent());
    }
    @Test void groundAccelerationUsesNativeMinimumRatherThanHeroSpeed() {
        Motion motion=new Motion(Retail.DATA);
        motion.step(DT,new Controls(Controls.FORWARD,0,0,0),floor(new V3[]{V3.ZERO}),0,-1,true);
        assertEquals(400*.0254*4*DT,motion.velocity.z(),1e-7);
    }
    @Test void airAccelerationUsesClampedWishSpeed() {
        Motion motion=new Motion(Retail.DATA);motion.grounded=false;
        motion.step(DT,new Controls(Controls.FORWARD,0,0,0),air,0,-1,true);
        assertEquals(10*80*.0254*DT*(1-.2*DT),motion.velocity.z(),1e-7);
    }
    @Test void ascendingCharacterUsesAirJumpInsteadOfFreeCoyoteJump() {
        Motion motion=new Motion(Retail.DATA);motion.grounded=false;motion.lastGroundTime=0;motion.velocity=new V3(0,3,9);
        motion.step(DT,new Controls(Controls.JUMP,Controls.JUMP,0,0),air,0,-1,true);
        assertTrue(motion.airJumpUsed);assertEquals(2,motion.stamina,1e-8);
        assertEquals(225*.0254-800*.0254*DT,motion.velocity.y(),1e-7);
        assertEquals(9*(1-.2*DT),motion.velocity.z(),1e-7);
    }
    @Test void proactiveSideContactPreservesTangentialMomentum() {
        Motion motion=new Motion(Retail.DATA);motion.grounded=false;motion.velocity=new V3(0,0,12);
        Motion.Collision side=new Motion.Collision() {
            public V3 wallContact(){return new V3(-1,0,0);}
            public Motion.MoveResult move(V3 delta,boolean crouch){return new Motion.MoveResult(delta,false,false,V3.ZERO,0);}
        };
        motion.step(DT,new Controls(Controls.JUMP,Controls.JUMP,0,0),side,0,-1,true);
        assertEquals(12*(1-.2*DT),motion.velocity.z(),1e-7);assertTrue(motion.velocity.x()<-6);assertEquals(1,motion.wallJumps);
    }
    @Test void wallJumpCombatPenaltiesStackAndExpire() {
        Motion motion=new Motion(Retail.DATA);motion.grounded=false;motion.wall=true;motion.wallNormal=new V3(-1,0,0);
        motion.step(DT,new Controls(Controls.JUMP,Controls.JUMP,0,0),air,0,-1,true);
        motion.wall=true;motion.step(DT,new Controls(Controls.JUMP,Controls.JUMP,0,0),air,0,-1,true);
        motion.stamina=1;double start=motion.stamina;
        motion.step(DT,Controls.NONE,air,0,-1,true);
        assertEquals(Retail.DATA.stat("EStaminaRegenPerSecond")*.75*.75*DT,motion.stamina-start,1e-9);
        motion.time+=5;motion.stamina=1;motion.step(DT,Controls.NONE,air,0,-1,true);
        assertEquals(Retail.DATA.stat("EStaminaRegenPerSecond")*DT,motion.stamina-1,1e-9);
    }
    @Test void airDashParryPreservesVerticalSpeedAndRecoversGravity() {
        Motion motion=new Motion(Retail.DATA);motion.grounded=false;motion.mode=Motion.Mode.DASH_AIR;motion.velocity=new V3(0,4,20);
        motion.step(DT,new Controls(Controls.PARRY,Controls.PARRY,0,0),air,0,-1,true);
        assertEquals(4-800*.0254*.3*DT,motion.velocity.y(),1e-7);
        assertEquals((8/.43*.4)*(1-.2*DT),motion.velocity.z(),1e-6);assertEquals(Motion.Mode.AIR,motion.mode);
    }
    @Test void dashJumpLandingWhileCrouchedEntersSlideWithMomentum() {
        Motion motion=new Motion(Retail.DATA);V3[] pos={V3.ZERO};var world=floor(pos);
        motion.step(DT,new Controls(Controls.FORWARD|Controls.DASH,Controls.DASH,0,0),world,0,-1,true);
        for(int i=1;i<21;i++)motion.step(DT,new Controls(Controls.FORWARD,0,0,0),world,0,-1,true);
        motion.step(DT,new Controls(Controls.FORWARD|Controls.JUMP,Controls.JUMP,0,0),world,0,-1,true);
        assertEquals(Motion.Mode.AIR,motion.mode);
        for(int i=0;i<100&&motion.mode!=Motion.Mode.SLIDE;i++)motion.step(DT,new Controls(Controls.FORWARD|Controls.CROUCH,0,0,0),world,0,-1,true);
        assertEquals(Motion.Mode.SLIDE,motion.mode);assertTrue(motion.velocity.horizontal()>13);
    }
    @Test void neutralDashDoesNotSpendACharge() {
        Motion motion=new Motion(Retail.DATA);motion.step(DT,new Controls(Controls.DASH,Controls.DASH,0,0),floor(new V3[]{V3.ZERO}),0,-1,true);
        assertEquals(3,motion.stamina);assertNotEquals(Motion.Mode.DASH_GROUND,motion.mode);
    }
}
