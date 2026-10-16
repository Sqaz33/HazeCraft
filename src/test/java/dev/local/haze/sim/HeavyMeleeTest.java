package dev.local.haze.sim;

import dev.local.haze.sim.movement.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class HeavyMeleeTest {
    private static final double DT=1.0/64;
    private static final Controls HOLD=new Controls(Controls.MELEE,0,0,0);
    private static MeleeController charge(boolean ground,boolean slide){
        var m=new MeleeController(Retail.DATA.movementProfile());
        for(int i=0;i<36;i++)m.step(DT,HOLD,ground,slide,true);
        return m;
    }
    @Test void shortTapStrikesOnceAndUsesLightCooldown(){
        var m=new MeleeController(Retail.DATA.movementProfile());
        m.step(DT,new Controls(Controls.MELEE,Controls.MELEE,0,0),true,false,true);
        for(int i=0;i<5;i++)m.step(DT,HOLD,true,false,true);
        m.step(DT,Controls.NONE,true,false,true);
        assertEquals(MeleeController.Type.LIGHT,m.type);assertEquals(MeleeController.Phase.ATTACKING,m.phase);
        assertEquals(1,m.strikeSerial);assertEquals(.75,m.cooldown(),1e-8);
        for(int i=0;i<30;i++)m.step(DT,Controls.NONE,true,false,true);
        assertEquals(1,m.strikeSerial);
    }
    @Test void heldQAutomaticallyStartsHeavyAndReleaseAfterLightWindowKeepsCommitment(){
        var held=charge(true,false);
        assertEquals(MeleeController.Phase.GROUND_DASH,held.phase);assertEquals(MeleeController.Type.HEAVY,held.type);
        assertEquals(0,held.strikeSerial);
        var released=new MeleeController(Retail.DATA.movementProfile());
        for(int i=0;i<20;i++)released.step(DT,HOLD,true,false,true);
        for(int i=20;i<36;i++)released.step(DT,Controls.NONE,true,false,true);
        assertEquals(MeleeController.Phase.GROUND_DASH,released.phase);
    }
    @Test void groundAirAndSlideChooseSeparateSourceAttacks(){
        assertEquals(MeleeController.Type.HEAVY,charge(true,false).type);
        assertEquals(MeleeController.Type.HEAVY_AIR,charge(false,false).type);
        var slide=charge(true,true);assertEquals(MeleeController.Type.SLIDE,slide.type);
        assertEquals(MeleeController.Phase.SLIDE_DASH,slide.phase);
        assertEquals(3000,Retail.number(slide.attack().get("m_flMovementAcc")));
        assertEquals(200*.0254,slide.knockUp(),1e-8);
    }
    @Test void sourceCurvePeakAccelerationAndTurnLimitDriveTheGroundLunge(){
        var m=charge(true,false);m.phaseStart=m.time-.2;
        var velocity=m.movement(V3.ZERO,DT);assertEquals(800*.0254,velocity.z(),1e-7);
        m.phaseStart=m.time;
        m.step(DT,new Controls(Controls.MELEE,0,90,0),true,false,true);
        assertEquals(700*DT,m.yaw,1e-5);assertTrue(m.direction.x()<0);assertTrue(m.direction.z()>0);
    }
    @Test void airLungeKeepsCapturedHeadingAndUsesNativeUpScaleThenLands(){
        var m=new MeleeController(Retail.DATA.movementProfile());
        for(int i=0;i<36;i++)m.step(DT,new Controls(Controls.MELEE,0,0,-45),false,false,true);
        m.phaseStart=m.time-.2;
        m.step(DT,new Controls(Controls.MELEE,0,90,0),false,false,true);
        assertEquals(0,m.yaw);assertEquals(-45,m.pitch);
        var velocity=m.movement(V3.ZERO,DT);assertEquals(0,velocity.x(),1e-8);
        assertEquals(800*.0254/Math.sqrt(2),velocity.z(),1e-6);
        assertEquals(velocity.z()*.1,velocity.y(),1e-6);
        m.step(DT,HOLD,true,false,true);assertEquals(MeleeController.Phase.GROUND_DASH,m.phase);
        assertEquals(0,m.direction.y(),1e-8);assertEquals(MeleeController.Type.HEAVY_AIR,m.type);
    }
    @Test void impactAndTimeoutStrikeOnceAndHitChangesRecoveryCooldown(){
        var m=charge(true,false);m.strike();m.strike();
        assertEquals(1,m.strikeSerial);assertEquals(1.3,m.cooldown(),1e-8);
        m.confirmedHit(true);assertEquals(1,m.cooldown(),1e-8);
        for(int i=0;i<30;i++)m.step(DT,HOLD,true,false,true);
        assertEquals(1,m.strikeSerial);assertEquals(MeleeController.Phase.NONE,m.phase);
        var miss=charge(true,false);
        for(int i=0;i<27;i++)miss.step(DT,HOLD,true,false,true);
        assertEquals(MeleeController.Phase.ATTACKING,miss.phase);assertEquals(1,miss.strikeSerial);
    }
    @Test void motionLungesWithoutStaminaCostAndCollisionEndsTheDash(){
        Motion motion=new Motion(Retail.DATA);double[] distance={0};
        Motion.Collision wall=(delta,crouch)->{
            double dz=Math.min(delta.z(),.8-distance[0]);boolean hit=dz<delta.z()-1e-8;distance[0]+=dz;
            return new Motion.MoveResult(new V3(delta.x(),0,dz),true,hit,hit?new V3(0,0,-1):V3.ZERO,0);
        };
        for(int i=0;i<60;i++)motion.step(DT,HOLD,wall,0,-1);
        assertEquals(.8,distance[0],1e-8);assertEquals(1,motion.melee.strikeSerial);
        assertEquals(motion.maxStamina(),motion.stamina,1e-8);assertTrue(motion.velocity.finite());
        assertEquals(0,motion.velocity.horizontal(),1e-8);
    }
    @Test void phaseClipsResolveAndFullBodyMeleeDoesNotStackKnifeReloadOrGunLayers(){
        var asset=SkinnedAsset.shared();String p="models/heroes_staging/haze/clips/";
        for(String clip:new String[]{"melee_start","melee_in_air_start","melee_dash","melee_in_air_dash","melee_hit","melee_in_air_hit","melee_quick_1"}){
            assertTrue(asset.clips.containsKey(p+clip));
            for(double age:new double[]{0,.1,.2,.35,.55}){
                var layers=HazePose.layers(asset,p+clip,age,false,Motion.Mode.MELEE,false,false,false,45,0,.1,2,.05,.05);
                assertTrue(layers.isEmpty(),clip);
                for(var transform:asset.pose(p+clip,age,false,layers))assertEquals(1,transform.determinant(),.004,clip);
            }
        }
        assertNull(MeleeVisuals.clip("CHARGING","NONE",.05,100,.1,true));
        assertEquals("melee_in_air_dash",MeleeVisuals.clip("AIR_DASH","HEAVY_AIR",.2,100,.1,false));
    }
}
