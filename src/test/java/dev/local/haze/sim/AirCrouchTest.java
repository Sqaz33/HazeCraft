package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AirCrouchTest {
    @Test void airControlDoesNotCrouchTheCapsuleOrCameraPose(){
        Motion m=new Motion(Retail.DATA);m.grounded=false;
        Motion.Collision air=(delta,crouch)->{assertFalse(crouch);return new Motion.MoveResult(delta,false,false,V3.ZERO,0);};
        m.step(.015625,new Controls(Controls.CROUCH,Controls.CROUCH,0,0),air,0,-1);
        assertFalse(m.ducked);assertFalse(m.crouchedPose());assertEquals(Motion.Mode.AIR,m.mode);
    }
    @Test void jumpLeavesCrouchEvenAtZeroTimeInputBoundary(){
        Motion m=new Motion(Retail.DATA);m.ducked=true;
        Motion.Collision air=(delta,crouch)->new Motion.MoveResult(delta,false,false,V3.ZERO,0);
        m.step(0,new Controls(Controls.CROUCH|Controls.JUMP,Controls.JUMP,0,0),air,0,-1);
        assertFalse(m.grounded);assertFalse(m.ducked);assertFalse(m.crouchedPose());
        m.step(.015625,new Controls(Controls.CROUCH,0,0,0),air,0,-1);
        assertFalse(m.ducked);
    }
    @Test void groundCrouchAndDownwardAirDashRemainAvailable(){
        Motion m=new Motion(Retail.DATA);
        Motion.Collision floor=(delta,crouch)->new Motion.MoveResult(delta.withY(0),true,false,V3.ZERO,0);
        m.step(.015625,new Controls(Controls.CROUCH,0,0,0),floor,0,-1);
        assertTrue(m.ducked);assertTrue(m.crouchedPose());
        m.grounded=false;
        m.step(.015625,new Controls(Controls.CROUCH,Controls.DOWN_DASH,0,0),(delta,crouch)->{
            assertFalse(crouch);return new Motion.MoveResult(delta,false,false,V3.ZERO,0);
        },0,-1);
        assertTrue(m.downDashUsed);assertFalse(m.ducked);assertFalse(m.crouchedPose());assertTrue(m.velocity.y()<-12);
    }
    @Test void lowCeilingForcesSafeGeometryWithoutAnAirCrouchPose(){
        Motion m=new Motion(Retail.DATA);m.grounded=false;
        Motion.Collision ceiling=new Motion.Collision(){
            public boolean canStand(){return false;}
            public Motion.MoveResult move(V3 delta,boolean crouch){assertTrue(crouch);return new Motion.MoveResult(delta,false,false,V3.ZERO,0);}
        };
        m.step(.015625,Controls.NONE,ceiling,0,-1);assertTrue(m.ducked);assertFalse(m.crouchedPose());
    }
}
