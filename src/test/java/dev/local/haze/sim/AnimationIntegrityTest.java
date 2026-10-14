package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class AnimationIntegrityTest {
    private static final String P="models/heroes_staging/haze/clips/";
    private static final SkinnedAsset A=SkinnedAsset.shared();
    @Test void neutralHeadingSamplesMiddleOfOriginalAimSweep(){
        String clip=P+"aim_weapon_idle";
        assertEquals(A.clips.get(clip).duration()/2,AnimationSource.headingTime(A,clip,0));
        var base=A.pose(P+"weapon_stand_idle",.25,false);
        var aimed=A.pose(P+"weapon_stand_idle",.25,false,List.of(AnimationSource.aim(A,"haze","idle",0,0)));
        Set<Integer> deforming=new HashSet<>();
        for(var primitive:A.primitives){var vertices=primitive.vertices();for(int k=0;k<vertices.weights().length;k++)if(vertices.weights()[k]>0)deforming.add(A.skins[primitive.skin()].joints()[vertices.joints()[k]]);}
        for(int i:deforming){
            float[] a=base[i].get(new float[16]),b=aimed[i].get(new float[16]);
            for(int k=0;k<16;k++)assertEquals(a[k],b[k],.002,A.nodes[i].name());
        }
    }
    @Test void daggerKeepsLocomotionLegTransformsAndUsesNativeMask(){
        var mask=AnimationSource.mask("Ability_Dagger");assertEquals(.08F,mask.get("pelvis"));assertFalse(mask.containsKey("leg_lower_L"));
        for(String movement:List.of("weapon_run_n","weapon_run_s","weapon_run_e","weapon_run_w","dash_ground","dash_air_left","slide_loop","in_air_loop_down")){
            for(double t:new double[]{0,.05,.1,.2,.35,.55,.7}){
                var base=A.pose(P+movement,t,false);
                var layered=A.pose(P+movement,t,false,List.of(AnimationSource.dagger(t)));
                for(String bone:List.of("leg_upper_L","leg_lower_L","ankle_L","leg_upper_R","leg_lower_R","ankle_R")){
                    int i=node(bone),parent=A.nodes[i].parent();
                    Matrix4f expected=new Matrix4f(base[parent]).invert().mul(base[i]);
                    Matrix4f actual=new Matrix4f(layered[parent]).invert().mul(layered[i]);
                    assertTrue(expected.equals(actual,.0001F),movement+"/"+t+" altered leg local transform "+bone);
                }
            }
        }
    }
    @Test void movementWithAimKnifeReloadAndShotsDoesNotStretchCoreLimbs(){
        var rest=A.pose(P+"weapon_stand_idle",0,false);
        for(String movement:List.of("weapon_run_n","weapon_run_nw","weapon_run_s","weapon_run_e","weapon_crouch_run_w","dash_ground","dash_air_right","slide_loop","in_air_loop_down")){
            Motion.Mode mode=movement.startsWith("dash_air")?Motion.Mode.DASH_AIR:movement.startsWith("dash")?Motion.Mode.DASH_GROUND:movement.startsWith("slide")?Motion.Mode.SLIDE:movement.startsWith("in_air")?Motion.Mode.AIR:Motion.Mode.RUN;
            for(double pitch:new double[]{-89,-45,0,45,89})for(double t:new double[]{0,.05,.1,.2,.35,.55,.7})for(int action=0;action<3;action++){
                var layers=HazePose.layers(A,P+movement,t,false,mode,false,movement.contains("crouch"),true,pitch,0,action==0?t:-1,action==1?2-t:0,action==2?.05:100,action==2?.08:100);
                var pose=A.pose(P+movement,t,false,layers);
                for(String bone:List.of("arm_lower_L","hand_L","arm_lower_R","hand_R","leg_lower_L","ankle_L","leg_lower_R","ankle_R")){
                    int i=node(bone),parent=A.nodes[i].parent();double length=distance(pose[i],pose[parent]),original=distance(rest[i],rest[parent]);
                    assertEquals(original,length,.015,movement+" pitch="+pitch+" t="+t+" action="+action+" stretched "+bone);
                    assertEquals(1,pose[i].determinant(),.003,"Non-rigid bone "+bone);
                }
            }
        }
    }
    private static int node(String name){for(int i=0;i<A.nodes.length;i++)if(A.nodes[i].name().equals(name))return i;throw new AssertionError(name);}
    @Test void locomotionDoesNotTranslateRenderedBodyAwayFromPhysicalPlayer(){
        for(String clip:List.of("weapon_run_n","weapon_run_s","weapon_run_e","weapon_run_w","out_of_combat_run_n","dash_ground","dash_air_forward","slide_loop","mantle_32","mantle_64","mantle_96","mantle_128")){
            double duration=A.clips.get(P+clip).duration();
            for(double fraction:new double[]{0,.25,.5,.75,1}){
                var pose=A.pose(P+clip,duration*fraction,false);
                var pelvis=pose[node("pelvis")].getTranslation(new Vector3f());
                assertTrue(Math.hypot(pelvis.x,pelvis.z)<.75,clip+" renders the body away from the player: "+pelvis);
                for(var mesh:A.skin(pose))for(float coordinate:mesh.position())assertTrue(Float.isFinite(coordinate)&&Math.abs(coordinate)<3,clip+" mesh left the actor origin");
            }
        }
    }
    @Test void originalBlendSpaceVerticesAndDirectionBoundariesRemainContinuous(){
        var spaces=AnimationSource.DATA.getAsJsonObject("locomotion");
        for(var entry:spaces.entrySet()){
            var space=entry.getValue().getAsJsonObject();var points=space.getAsJsonArray("points");var clips=space.getAsJsonArray("clips");
            for(int i=0;i<points.size();i++){
                var point=points.get(i).getAsJsonArray();var samples=LocomotionBlend.sample(entry.getKey(),point.get(0).getAsDouble(),point.get(1).getAsDouble());
                assertEquals(1,samples.size());assertEquals(clips.get(i).getAsString(),samples.getFirst().clip());assertEquals(1,samples.getFirst().weight());
            }
            for(int degrees=-180;degrees<180;degrees+=3){
                double angle=Math.toRadians(degrees);var samples=LocomotionBlend.sample(entry.getKey(),Math.sin(angle)*1000,Math.cos(angle)*1000);
                double total=samples.stream().mapToDouble(LocomotionBlend.Sample::weight).sum();assertEquals(1,total,1e-6);
                for(var sample:samples)assertTrue(sample.weight()>=0&&Float.isFinite(sample.weight()));
            }
        }
        for(int degrees=-180;degrees<=180;degrees+=5){
            V3 velocity=V3.look(degrees,0).mul(8.2);var plan=LocomotionBlend.pose(A,"weapon_run",velocity,0,.4);
            var pose=A.pose(plan.clip(),plan.time(),true,plan.layers());assertTrue(pose[node("pelvis")].getTranslation(new Vector3f()).length()<2);
        }
    }
    private static double distance(Matrix4f a,Matrix4f b){return a.getTranslation(new Vector3f()).distance(b.getTranslation(new Vector3f()));}
}
