package dev.local.haze.sim;

import com.google.gson.*;
import java.nio.file.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CameraHeightBlendTest {
    @Test void matches192NativeCasesAndReversalTraces() throws Exception {
        JsonObject data=JsonParser.parseString(Files.readString(Path.of("D:/DeadlockHazeLab/research/25950029/camera/native-blend-cases.json"))).getAsJsonObject();
        assertEquals("25950029",data.get("build").getAsString());
        assertEquals("fadaf78301af6d47d286de6199d1fd0aba7053f6ab597afa3de55369f72424c6",data.get("sourceSha256").getAsString());
        assertEquals(192,data.getAsJsonArray("cases").size());
        for(var e:data.getAsJsonArray("cases")){
            var c=e.getAsJsonObject();assertEquals(c.get("output").getAsFloat(),CameraHeightBlend.approach(c.get("current").getAsFloat(),c.get("target").getAsFloat(),c.get("strength").getAsFloat(),c.get("dt").getAsFloat()),1e-7,c.toString());
        }
        for(var e:data.getAsJsonArray("traces")){
            var trace=e.getAsJsonObject();float current=trace.get("initial").getAsFloat();
            for(var f:trace.getAsJsonArray("frames")){
                var frame=f.getAsJsonObject();current=CameraHeightBlend.approach(current,frame.get("target").getAsFloat(),trace.get("strength").getAsFloat(),trace.get("dt").getAsFloat());
                assertEquals(frame.get("output").getAsFloat(),current,1e-7);
            }
        }
    }
    @Test void crouchIsContinuousAndReversesFromCurrentHeight(){
        var camera=new CameraHeightBlend(80);camera.advanceTo(0,false);
        camera.advanceTo(.01,true);float first=camera.height(86,55);assertTrue(first<86&&first>55);
        camera.advanceTo(.02,true);float second=camera.height(86,55);assertTrue(second<first);
        camera.advanceTo(.02,false);assertEquals(second,camera.height(86,55));
        camera.advanceTo(.025,false);assertTrue(camera.height(86,55)>second);assertTrue(camera.height(86,55)<86);
        camera.advanceTo(.2,false);assertEquals(86,camera.height(86,55));
    }
    @Test void repeatedAndOlderFrameReadsDoNotAdvanceCamera(){
        var camera=new CameraHeightBlend(80);camera.advanceTo(10,false);camera.advanceTo(10.01,true);float height=camera.height(86,55);
        for(int i=0;i<10;i++){camera.advanceTo(10.01,true);camera.advanceTo(10,true);assertEquals(height,camera.height(86,55));}
        camera.advanceTo(10.2,true);assertEquals(55,camera.height(86,55));
        camera.reset();camera.advanceTo(0,false);assertEquals(86,camera.height(86,55));
    }
    @Test void renderFramePartitionPreservesNativeTransitionDuration(){
        for(int fps:new int[]{30,60,120,240}){
            var camera=new CameraHeightBlend(80);camera.advanceTo(0,false);
            for(int i=1;i<=fps;i++)camera.advanceTo(i*.1/fps,true);
            assertEquals(.0111456,camera.standing(),2e-6);
            camera.advanceTo(.12,true);assertEquals(0,camera.standing());
        }
        assertEquals(80,Retail.DATA.nativeFloat("citadel_camera_height_approach_speed"));
        assertEquals(86,Retail.DATA.camera.get("m_flCameraHeightStanding").getAsFloat());
        assertEquals(55,Retail.DATA.camera.get("m_flCameraHeightCrouching").getAsFloat());
    }
}
