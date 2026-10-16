package dev.local.haze.sim;

import dev.local.haze.sim.movement.CapsuleMover;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CapsuleCollisionTest {
    private static CapsuleMover mover(){return new CapsuleMover(32*.0254,20*.0254,80*.0254,8*.0254,.2,10,1,16);}
    @Test void fastSweepStopsAtThinWallAndRetainsTangent(){
        var mover=mover();var wall=new CapsuleMover.Box(2,-10,-10,2.01,10,10);
        var result=mover.resolve(new V3(0,0,0),new V3(30,0,4),false,b->List.of(wall));
        assertEquals(2-20*.0254,result.position().x(),1e-6);assertEquals(4,result.position().z(),1e-6);
        assertEquals(0,result.velocity().x(),1e-6);assertEquals(4,result.velocity().z(),1e-6);
    }
    @Test void roundCornerReturnsObliqueContactRatherThanTwoBlockedAxes(){
        var mover=mover();var corner=new CapsuleMover.Box(2,-10,2,3,10,3);
        var hit=CapsuleMover.sweep(mover.shape(false),V3.ZERO,new V3(4,0,4),corner);
        assertNotNull(hit);assertEquals(-Math.sqrt(.5),hit.normal().x(),1e-6);assertEquals(hit.normal().x(),hit.normal().z(),1e-6);
    }
    @Test void stationarySideProbeFindsTheNearbyWall(){
        var wall=new CapsuleMover.Box(.6,-10,-10,2,10,10);
        assertEquals(new V3(-1,0,0),mover().wallContact(V3.ZERO,false,b->List.of(wall)));
    }
    @Test void sourceCapsuleKeepsCrouchGeometrySeparateFromCameraHeight(){
        var mover=mover();assertEquals(1.016,2*mover.shape(false).radius(),1e-9);
        assertEquals(2.54,mover.shape(false).bounds(V3.ZERO).maxY(),1e-9);
        assertEquals(72*.0254,mover.shape(true).bounds(V3.ZERO).maxY(),1e-9);
        var ceiling=new CapsuleMover.Box(-2,2,-2,2,3,2);
        assertTrue(CapsuleMover.contact(mover.shape(false),V3.ZERO,ceiling).separation()<0);
        assertTrue(CapsuleMover.contact(mover.shape(true),V3.ZERO,ceiling).separation()>0);
    }
    @Test void sideWallCannotTurnSpaceIntoAForwardMantle(){
        Motion motion=new Motion(Retail.DATA);motion.grounded=false;
        Motion.Collision world=new Motion.Collision(){
            public V3 wallContact(){return new V3(-1,0,0);}
            public V3 mantle(V3 direction,double height){throw new AssertionError("Side contact must not request a forward mantle");}
            public Motion.MoveResult move(V3 delta,boolean crouch){return new Motion.MoveResult(delta,false,false,V3.ZERO,0);}
        };
        motion.step(1.0/64,new Controls(0,Controls.JUMP,0,0),world,0,-1,true);
        assertEquals(Motion.JumpKind.WALL,motion.jumpKind);assertEquals(1,motion.wallJumps);assertTrue(motion.velocity.x()<-6);assertTrue(motion.velocity.y()>13);
    }
    @Test void supportSpringHoldsFloorAndSpaceInAirBouncesWithoutGroundReset(){
        var mover=mover();V3[] position={new V3(10,64,10)};
        var floor=new CapsuleMover.Box(-100,63,-100,100,64,100);
        Motion motion=new Motion(Retail.DATA);
        Motion.Collision world=new Motion.Collision(){
            public Motion.MoveResult move(V3 delta,boolean crouch){throw new AssertionError("Missing velocity adapter");}
            public Motion.MoveResult move(V3 delta,boolean crouch,V3 velocity,double dt){
                V3 before=position[0];var result=mover.move(before,velocity,motion.grounded,crouch,dt,b->List.of(floor));position[0]=result.position();
                return new Motion.MoveResult(position[0].sub(before),result.grounded(),false,V3.ZERO,result.slope(),result.velocity());
            }
        };
        for(int i=0;i<64;i++)motion.step(1.0/64,Controls.NONE,world,0,-1,true);
        assertEquals(64,position[0].y(),1e-6);assertTrue(motion.grounded);
        motion.step(1.0/64,new Controls(0,Controls.JUMP,0,0),world,0,-1,true);
        assertFalse(motion.grounded);
        for(int i=0;i<12;i++)motion.step(1.0/64,Controls.NONE,world,0,-1,true);
        motion.step(1.0/64,new Controls(0,Controls.JUMP,0,0),world,0,-1,true);
        assertEquals(13.335-800*.0254/64,motion.velocity.y(),1e-5);assertTrue(motion.airJumpUsed);assertEquals(2,motion.jumpSerial);
        double apex=position[0].y();for(int i=0;i<160;i++){motion.step(1.0/64,Controls.NONE,world,0,-1,true);apex=Math.max(apex,position[0].y());}
        assertTrue(apex>68.5);assertTrue(motion.grounded);assertFalse(motion.airJumpUsed);assertEquals(64,position[0].y(),1e-5);
    }
}
