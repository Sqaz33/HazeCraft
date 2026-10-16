package dev.local.haze.sim;

import dev.local.haze.sim.movement.MantleExit;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MantleSlideTest {
    private static final double DT=1.0/64,BOOST=400*.0254;
    private static final V3 FORWARD=new V3(0,0,1);
    private static final class Ledge implements Motion.Collision {
        V3 position=V3.ZERO;
        public V3 wallContact(){return position.y()<.1?new V3(0,0,-1):V3.ZERO;}
        public V3 mantle(V3 direction,double height){return new V3(0,64*.0254,.5);}
        public Motion.MoveResult move(V3 delta,boolean crouch){
            if(position.z()+delta.z()>=.5-1e-5&&position.y()>=64*.0254-1e-5&&delta.y()<0)
                delta=delta.withY(64*.0254-position.y());
            position=position.add(delta);
            return new Motion.MoveResult(delta,position.y()>=64*.0254-1e-5&&position.z()>=.5-1e-5,false,V3.ZERO,0);
        }
    }
    private static Motion climb(Ledge world,int held){
        Motion m=new Motion(Retail.DATA);m.grounded=false;
        m.step(DT,new Controls(Controls.FORWARD|Controls.JUMP,Controls.JUMP,0,0),world,0,-1);
        assertEquals(Motion.Mode.MANTLE,m.mode);
        for(int i=0;i<100&&m.mode==Motion.Mode.MANTLE;i++)m.step(DT,new Controls(held,0,0,0),world,0,-1);
        assertNotEquals(Motion.Mode.MANTLE,m.mode);return m;
    }
    @Test void currentNativeConstructorBoostIsAvailableForEveryMantleType(){
        for(var element:Retail.DATA.ability("citadel_ability_mantle").getAsJsonArray("m_vecMantleTypes"))
            assertEquals(400,element.getAsJsonObject().get("m_flSlideExitBoostOnGround").getAsDouble());
        assertEquals(10.16,BOOST,1e-10);
    }
    @Test void exitCapsOldMomentumBeforeAddingBoostAlongHeldInput(){
        var result=MantleExit.velocity(new V3(0,0,20),FORWARD,FORWARD,8.2,BOOST,true,true);
        assertEquals(18.36,result.z(),1e-8);assertEquals(0,result.y());
        V3 diagonal=new V3(1,0,1).normalized();
        var angled=MantleExit.velocity(V3.ZERO,diagonal,FORWARD,8.2,BOOST,true,true);
        assertEquals(BOOST,angled.horizontal(),1e-8);assertEquals(angled.x(),angled.z(),1e-8);
    }
    @Test void releaseNeutralBackwardAndAirExitDoNotReceiveCrouchBoost(){
        var old=new V3(0,0,4);
        assertEquals(old,MantleExit.velocity(old,FORWARD,FORWARD,8.2,BOOST,false,true));
        assertEquals(old,MantleExit.velocity(old,V3.ZERO,FORWARD,8.2,BOOST,true,true));
        assertEquals(old,MantleExit.velocity(old,FORWARD.mul(-1),FORWARD,8.2,BOOST,true,true));
        assertEquals(old,MantleExit.velocity(old,FORWARD,FORWARD,8.2,BOOST,true,false));
    }
    @Test void heldCrouchDuringClimbEntersSustainedSlideWithoutSpendingStamina(){
        var world=new Ledge();var m=climb(world,Controls.FORWARD|Controls.CROUCH);
        assertEquals(Motion.Mode.SLIDE,m.mode);assertTrue(m.grounded);assertTrue(m.crouchedPose());
        assertTrue(m.velocity.horizontal()>BOOST);assertEquals(m.maxStamina(),m.stamina,1e-8);
        double start=world.position.z(),speed=m.velocity.horizontal();
        for(int i=0;i<8;i++)m.step(DT,new Controls(Controls.FORWARD|Controls.CROUCH,0,0,0),world,0,-1);
        assertEquals(Motion.Mode.SLIDE,m.mode);assertTrue(world.position.z()>start+1);
        assertTrue(m.velocity.horizontal()<speed);assertTrue(m.velocity.horizontal()>8.89);
    }
    @Test void ordinaryMantleKeepsTrajectoryAndHasNoAddedImpulse(){
        var world=new Ledge();var m=climb(world,Controls.FORWARD);
        assertEquals(Motion.Mode.RUN,m.mode);assertTrue(m.grounded);assertFalse(m.crouchedPose());
        assertEquals(64*.0254,world.position.y(),1e-5);assertEquals(.5,world.position.z(),1e-5);
        assertTrue(m.velocity.horizontal()>0&&m.velocity.horizontal()<=8.2);
    }
}
