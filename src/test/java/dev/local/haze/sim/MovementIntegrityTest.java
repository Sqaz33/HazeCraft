package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MovementIntegrityTest {
    private static final Retail D=Retail.DATA;
    private static final double DT=1.0/120;
    private static Motion.Collision ground(V3[] position){return(delta,crouch)->{V3 next=position[0].add(delta);boolean grounded=next.y()<=0;V3 actual=grounded?delta.withY(-position[0].y()):delta;position[0]=position[0].add(actual);return new Motion.MoveResult(actual,grounded,false,V3.ZERO,0);};}
    private static Motion.Collision air(V3[] position){return(delta,crouch)->{position[0]=position[0].add(delta);return new Motion.MoveResult(delta,false,false,V3.ZERO,0);};}
    @Test void eightDashDirectionsHaveSameRetailDistanceAtAnyHeading(){
        int[] directions={Controls.FORWARD,Controls.FORWARD|Controls.RIGHT,Controls.RIGHT,Controls.BACK|Controls.RIGHT,Controls.BACK,Controls.BACK|Controls.LEFT,Controls.LEFT,Controls.FORWARD|Controls.LEFT};
        for(float yaw:new float[]{0,37,90,180,-125})for(int direction:directions){
            Motion m=new Motion(D);V3[] position={V3.ZERO};var collision=ground(position);
            for(int i=0;i<75;i++)m.step(DT,new Controls(direction|(i==0?Controls.DASH:0),i==0?Controls.DASH:0,yaw,0),collision,0,-1,true);
            assertEquals(10,position[0].horizontal(),1e-8,"direction "+direction+" yaw "+yaw);
            assertEquals(1,position[0].normalized().dot(new Controls(direction,0,yaw,0).wish()),1e-8);
        }
    }
    @Test void airDashUsesItsOwnDistanceAndOnlyOneChargeUntilGround(){
        Motion m=new Motion(D);m.grounded=false;V3[] position={V3.ZERO};var collision=air(position);
        m.step(DT,new Controls(Controls.FORWARD|Controls.DASH,Controls.DASH,0,0),collision,0,-1,true);
        for(int i=1;i<52;i++)m.step(DT,new Controls(Controls.FORWARD,0,0,0),collision,0,-1,true);
        assertEquals(8,position[0].z(),1e-8);assertEquals(0,position[0].y(),1e-8);assertTrue(m.airDashUsed);
        double start=m.dashStart,stamina=m.stamina;
        m.step(DT,new Controls(Controls.DASH,Controls.DASH,0,0),collision,0,-1,true);
        assertEquals(start,m.dashStart);assertTrue(m.stamina>=stamina);
    }
    @Test void dashJumpWindowConsumesAnAdditionalStaminaCharge(){
        Motion m=new Motion(D);V3[] position={V3.ZERO};var collision=ground(position);
        m.step(DT,new Controls(Controls.FORWARD|Controls.DASH,Controls.DASH,0,0),collision,0,-1,true);
        for(int i=1;i<40;i++)m.step(DT,new Controls(Controls.FORWARD,0,0,0),collision,0,-1,true);
        m.step(DT,new Controls(Controls.FORWARD|Controls.JUMP,Controls.JUMP,0,0),collision,0,-1,true);
        assertEquals(Motion.Mode.AIR,m.mode);assertTrue(m.stamina<1.1);
        assertTrue(m.velocity.horizontal()>25);assertTrue(m.velocity.y()>10);
    }
    @Test void dashSlideWaitsForRetailStartWindowAndKeepsSpeed(){
        Motion m=new Motion(D);V3[] position={V3.ZERO};var collision=ground(position);
        m.step(DT,new Controls(Controls.FORWARD|Controls.DASH,Controls.DASH,0,0),collision,0,-1,true);
        for(int i=1;i<35;i++)m.step(DT,new Controls(Controls.FORWARD|Controls.CROUCH,0,0,0),collision,0,-1,true);
        assertEquals(Motion.Mode.DASH_GROUND,m.mode);
        for(int i=35;i<38;i++)m.step(DT,new Controls(Controls.FORWARD|Controls.CROUCH,0,0,0),collision,0,-1,true);
        assertEquals(Motion.Mode.SLIDE,m.mode);assertTrue(m.velocity.horizontal()>16);
        double charge=m.stamina;m.step(DT,new Controls(Controls.FORWARD|Controls.CROUCH,0,0,0),collision,0,-1,true);assertTrue(m.stamina>=charge);
    }
    @Test void groundDashMaintainsContactWhenHostNeedsADownwardCollision(){
        Motion m=new Motion(D);V3[] position={V3.ZERO};
        Motion.Collision host=(delta,crouch)->{boolean contact=position[0].y()+delta.y()<0;V3 actual=contact?delta.withY(-position[0].y()):delta;position[0]=position[0].add(actual);return new Motion.MoveResult(actual,contact,false,V3.ZERO,0);};
        m.step(DT,new Controls(Controls.FORWARD|Controls.DASH,Controls.DASH,0,0),host,0,-1,true);
        for(int i=1;i<35;i++){m.step(DT,new Controls(Controls.FORWARD|Controls.CROUCH,0,0,0),host,0,-1,true);assertTrue(m.grounded,"Lost floor contact during dash substep "+i);}
        for(int i=35;i<38;i++)m.step(DT,new Controls(Controls.FORWARD|Controls.CROUCH,0,0,0),host,0,-1,true);
        assertEquals(Motion.Mode.SLIDE,m.mode);
    }
    @Test void wallJumpPushesAwayWithoutUsingStamina(){
        Motion m=new Motion(D);m.grounded=false;m.wall=true;m.wallNormal=new V3(0,0,-1);V3[] position={V3.ZERO};
        m.step(DT,new Controls(Controls.JUMP,Controls.JUMP,0,0),air(position),0,-1,true);
        assertTrue(m.velocity.z()<-7);assertTrue(m.velocity.y()>13);assertEquals(3,m.stamina);assertFalse(m.grounded);
    }
    @Test void repeatedWallJumpUsesNativeCostAfterTheFreeFirstJump(){
        Motion m=new Motion(D);m.grounded=false;m.wall=true;m.wallNormal=new V3(0,0,-1);V3[] position={V3.ZERO};var collision=air(position);
        m.step(DT,new Controls(Controls.JUMP,Controls.JUMP,0,0),collision,0,-1,true);assertEquals(3,m.stamina);assertEquals(1,m.wallJumps);
        m.wall=true;m.step(DT,new Controls(Controls.JUMP,Controls.JUMP,0,0),collision,0,-1,true);assertEquals(2.5,m.stamina,1e-8);assertEquals(2,m.wallJumps);
        m.stamina=.2;m.wall=true;m.step(DT,new Controls(Controls.JUMP,Controls.JUMP,0,0),collision,0,-1,true);assertEquals(2,m.wallJumps);
    }
    @Test void wallJumpFatigueUsesDecodedRemapAndRationalBias(){
        assertEquals(.2/(3*.8+1),SourceMath.wallJumpPower(D,0),1e-7);
        assertEquals(.6/(3*.4+1),SourceMath.wallJumpPower(D,.625),1e-7);
        assertEquals(1,SourceMath.wallJumpPower(D,1.25));
        assertEquals(1,SourceMath.wallJumpPower(D,100));
        assertEquals(.2,SourceMath.bias(.5F,.2F),1e-7);
    }
    @Test void blockedDashDoesNotTeleportThroughWall(){
        Motion m=new Motion(D);V3[] position={V3.ZERO};
        Motion.Collision wall=(delta,crouch)->{double dz=Math.min(delta.z(),2-position[0].z());V3 actual=new V3(delta.x(),0,dz);position[0]=position[0].add(actual);return new Motion.MoveResult(actual,true,dz<delta.z(),new V3(0,0,-1),0);};
        for(int i=0;i<75;i++)m.step(DT,new Controls(Controls.FORWARD|(i==0?Controls.DASH:0),i==0?Controls.DASH:0,0,0),wall,0,-1,true);
        assertEquals(2,position[0].z(),1e-9);assertEquals(0,m.velocity.z(),1e-9);
    }
    @Test void mantleRisesBeforeTraversingTheLedge(){
        Motion m=new Motion(D);m.grounded=false;m.wall=true;m.wallNormal=new V3(0,0,-1);V3[] position={V3.ZERO};
        Motion.Collision ledge=new Motion.Collision(){
            public V3 mantle(V3 direction,double maxHeight){return new V3(0,2,1);}
            public Motion.MoveResult move(V3 delta,boolean crouch){
                double dz=position[0].y()<2-1e-6?0:delta.z(),dy=position[0].z()>=1?Math.max(delta.y(),2-position[0].y()):delta.y();V3 actual=new V3(delta.x(),dy,dz);position[0]=position[0].add(actual);return new Motion.MoveResult(actual,position[0].y()>=2&&position[0].z()>=1,false,V3.ZERO,0);
            }
        };
        m.step(DT,new Controls(Controls.FORWARD|Controls.JUMP,Controls.JUMP,0,0),ledge,0,-1,true);
        assertEquals(Motion.Mode.MANTLE,m.mode);assertEquals(64,m.mantleHeight);
        for(int i=0;i<36;i++){m.step(DT,new Controls(Controls.FORWARD,0,0,0),ledge,0,-1,true);assertEquals(0,position[0].z(),1e-9);}
        for(int i=0;i<25;i++)m.step(DT,new Controls(Controls.FORWARD,0,0,0),ledge,0,-1,true);
        assertEquals(2,position[0].y(),1e-7);assertTrue(position[0].z()>=1);assertNotEquals(Motion.Mode.MANTLE,m.mode);
    }
}
