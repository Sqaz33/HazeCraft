package dev.local.haze.sim;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.joml.Matrix4f;

class RetailSimulationTest {
    private static final Retail D=Retail.DATA;
    @Test void pinnedOctoberBuildAndNativeDefaults(){
        assertEquals("25917577",D.root.get("build").getAsString());
        assertEquals(730,D.stat("EMaxHealth"));assertEquals(8.2,D.stat("EMaxMoveSpeed"));
        assertEquals(800,D.nativeFloat("sv_gravity"));assertEquals(90,D.nativeFloat("citadel_camera_hero_fov"));
        assertEquals(103,D.camera.get("m_flCameraBackOffset").getAsDouble());
        assertEquals(-34,D.camera.get("m_flCameraSideOffset").getAsDouble());
    }
    @Test void extractedUpgradeTablesAreCumulative(){
        assertEquals(13,D.prop(Retail.SIGNATURES[0],"AbilityCooldown",3));
        assertEquals(3.75,D.prop(Retail.SIGNATURES[0],"SleepDuration",2));
        assertEquals(15,D.prop(Retail.SIGNATURES[0],"FixationStacks",2));
        assertEquals(7,D.prop(Retail.SIGNATURES[1],"InvisMoveSpeedMod",1));
        assertEquals(2,D.prop(Retail.SIGNATURES[1],"AbilityCharges",2));
        assertEquals(80,D.prop(Retail.SIGNATURES[2],"MaxStacks",2));
        assertEquals(0.31,D.prop(Retail.SIGNATURES[2],"DamageBonusFixedPerStack",3),1e-12);
        assertEquals(100,D.prop(Retail.SIGNATURES[3],"AbilityCooldown",3));
        assertEquals(70,D.prop(Retail.SIGNATURES[3],"EvasionPercent",3));
    }
    @Test void spiritUpgradeScalesRemainSeparateFromBase(){
        assertEquals(120,D.prop(Retail.SIGNATURES[2],"ProcDamage",1,100,0));
        assertEquals(285,D.prop(Retail.SIGNATURES[0],"Damage",0,100,0));
        assertEquals(18,D.prop(Retail.SIGNATURES[1],"AbilityDuration",0,100,0));
    }
    @Test void fireCadenceDoesNotDependOnHostTickPartition(){
        List<Double>a=shots(0.05,2.0),b=shots(1.0/120,2.0);
        assertEquals(20,a.size());assertEquals(a.size(),b.size());
        for(int i=0;i<a.size();i++){assertEquals(i*0.105,a.get(i),1e-9);assertEquals(a.get(i),b.get(i),1e-9);}
    }
    private static List<Double>shots(double dt,double duration){
        Gun g=new Gun(D);List<Double>t=new ArrayList<>();
        for(int i=0;i<(int)Math.round(duration/dt);i++)for(Gun.Shot s:g.advance(dt,true,false,false,false,0,0,0))t.add(s.time());return t;
    }
    @Test void magazineReloadAndSlideAmmo(){
        Gun g=new Gun(D);for(int i=0;i<54;i++)g.advance(.05,true,false,false,false,0,0,0);
        assertEquals(0,g.ammo);assertTrue(g.reloadRemaining>0);
        for(int i=0;i<50;i++)g.advance(.05,false,false,false,false,0,0,0);
        assertEquals(25,g.ammo);
        for(int i=0;i<100;i++)g.advance(.05,true,false,false,true,0,0,0);
        assertEquals(25,g.ammo);
    }
    @Test void fixationClampsExpiresAndIsolatesTargets(){
        Fixation a=new Fixation(),b=new Fixation();
        for(int i=0;i<100;i++)a.bullet(D,0,false,1);
        assertEquals(40,a.stacks);assertEquals(0,b.stacks);a.expire(7.00001);assertEquals(0,a.stacks);
        assertEquals(3,b.bullet(D,0,true,1).addedStacks());
    }
    @Test void sleepMinimumAndDelayedWake(){
        Fixation f=new Fixation();f.dagger(D,0,1);assertTrue(f.asleep(1.05));
        f.damaged(D,0,1.05);assertTrue(f.asleep(1.19));assertFalse(f.asleep(1.21));
        f.dagger(D,2,2);assertEquals(15,f.stacks);assertTrue(f.asleep(5.74));assertFalse(f.asleep(5.76));
    }
    @Test void cooldownsAndUnlimitedUpgradeCommands(){
        Abilities a=new Abilities(D);assertTrue(a.cast(0));assertFalse(a.cast(0));
        a.advance(.19);assertFalse(a.consumeDagger());a.advance(.02);assertTrue(a.consumeDagger());assertFalse(a.consumeDagger());
        for(int i=0;i<4;i++){assertTrue(a.upgrade(i));assertTrue(a.upgrade(i));assertTrue(a.upgrade(i));assertFalse(a.upgrade(i));}
        assertFalse(a.upgrade(-1));assertFalse(a.cast(2));
    }
    @Test void daggerDebuffsBeginOnWakeAndRespectFixationRank(){
        Fixation f=new Fixation();f.dagger(D,3,0,0);assertEquals(15,f.stacks);
        assertEquals(0,f.bulletResistUntil);assertEquals(0,f.movementSlow(D,0,0.1));
        f.damaged(D,3,0.1);f.advance(D,0.19);assertEquals(0,f.bulletResistUntil);
        f.advance(D,0.21);assertEquals(6.2,f.bulletResistUntil,1e-10);assertEquals(-10,f.resistReduction);
        assertEquals(40,f.movementSlow(D,0,0.21));assertEquals(0,f.movementSlow(D,0,3.21));
        for(int i=0;i<35;i++)f.bullet(D,0,false,1);f.dagger(D,3,0,2);assertEquals(40,f.stacks);
    }
    @Test void cameraSequenceUsesOriginalCastAndChannelTargets(){
        var a=D.ability("ability_bullet_flurry");var cast=a.getAsJsonObject("m_cameraSequenceCastStart");
        assertEquals(103,CameraSequence.evaluate(cast.getAsJsonArray("m_vecDistanceOperations"),0,103,103),1e-9);
        assertEquals(120,CameraSequence.evaluate(cast.getAsJsonArray("m_vecDistanceOperations"),0.8,103,103),1e-9);
        assertEquals(60,CameraSequence.evaluate(cast.getAsJsonArray("m_vecFOVOperations"),0.8,90,90),1e-9);
        assertEquals(350,CameraSequence.evaluate(a.getAsJsonObject("m_cameraSequenceChannelStart").getAsJsonArray("m_vecDistanceOperations"),3,120,103),1e-9);
    }
    @Test void originalMuzzleAttachmentsPointForwardAfterSourceAxisConversion(){
        SkinnedAsset a=SkinnedAsset.shared();String prefix="models/heroes_staging/haze/clips/";
        var pose=a.pose(prefix+"weapon_stand_idle",0,false,List.of(new SkinnedAsset.Layer(prefix+"aim_weapon_idle",0,false,1,null,true)));
        V3 right=SkinnedAsset.worldOffset(a.attachment(pose,"muzzle_flash_r"),0),left=SkinnedAsset.worldOffset(a.attachment(pose,"muzzle_flash_l"),0);
        assertTrue(right.finite()&&left.finite());assertTrue(right.z()>0.3,"Right muzzle is not forward: "+right);assertTrue(left.z()>0.3,"Left muzzle is not forward: "+left);
        assertTrue(right.y()>1&&right.y()<2.5);assertTrue(left.y()>1&&left.y()<2.5);
    }
    @Test void dashIntegralUsesRetailHeroCurve(){
        Motion m=new Motion(D);V3[] pos={V3.ZERO};Motion.Collision ground=deltaGround(pos);
        m.step(1.0/120,new Controls(Controls.FORWARD|Controls.DASH,Controls.DASH,0,0),ground,0,-1);
        for(int i=1;i<75;i++)m.step(1.0/120,new Controls(Controls.FORWARD,0,0,0),ground,0,-1);
        assertEquals(10,pos[0].z(),1e-9);assertEquals(0,pos[0].x(),1e-9);
        assertTrue(m.stamina<3);assertEquals(.62,m.dashDuration);
    }
    @Test void groundJumpDoesNotConsumeStaminaAndAirJumpDoes(){
        Motion m=new Motion(D);V3[]pos={V3.ZERO};
        m.step(1.0/120,new Controls(Controls.JUMP,Controls.JUMP,0,0),deltaGround(pos),0,-1);
        assertEquals(3,m.stamina);assertTrue(m.velocity.y()>7);
        m.step(1.0/120,new Controls(Controls.JUMP,Controls.JUMP,0,0),deltaGround(pos),0,-1);
        assertTrue(m.stamina<2.01);assertTrue(m.airJumpUsed);
    }
    @Test void malformedTimeStepsRejected(){assertThrows(IllegalArgumentException.class,()->new Motion(D).step(1,Controls.NONE,deltaGround(new V3[]{V3.ZERO}),0,-1));}
    private static Motion.Collision deltaGround(V3[]pos){return(delta,crouch)->{V3 next=pos[0].add(delta);boolean ground=next.y()<=0;V3 actual=ground?new V3(delta.x(),-pos[0].y(),delta.z()):delta;pos[0]=pos[0].add(actual);return new Motion.MoveResult(actual,ground,false,V3.ZERO,0);};}
    @Test void allOriginalAnimationTracksDecodeAndStayFinite(){
        SkinnedAsset asset=SkinnedAsset.load();assertEquals(444,asset.clips.size());assertEquals(6,asset.primitives.length);assertEquals(124,asset.nodes.length);
        for(SkinnedAsset.Clip clip:asset.clips.values()) {
            Matrix4f[] pose=asset.pose(clip.name(),clip.duration()*0.5,false);
            for(Matrix4f matrix:pose)assertTrue(matrix.isFinite(),clip.name());
        }
        for(String name:List.of("primary_idle","primary_stand_reload","ability_sleep_dagger","ability_bullet_flurry","models/heroes_staging/haze/clips/dash_ground")){
            SkinnedAsset.Geometry[] geometry=asset.skin(asset.pose(name,0,false));
            for(SkinnedAsset.Geometry g:geometry)for(float v:g.position())assertTrue(Float.isFinite(v)&&Math.abs(v)<20,name);
        }
    }
}
