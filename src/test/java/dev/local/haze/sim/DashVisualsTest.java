package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DashVisualsTest {
    private static final String P="models/heroes_staging/haze/clips/";
    @Test void singleGroundClipFacesAllEightDashDirectionsAtAnyViewYaw(){
        for(float yaw:new float[]{-721,-180,-37,0,45,179,400})for(int direction=0;direction<8;direction++){
            V3 travel=V3.look(yaw+direction*45,0);
            assertEquals(1,V3.look(DashVisuals.yaw(P+"dash_ground",travel,yaw),0).dot(travel),1e-6);
        }
    }
    @Test void diagonalAirDashRetainsExactHeadingBetweenCardinalClips(){
        for(float view:new float[]{-179,0,33,180,721})for(int direction=0;direction<8;direction++){
            int quadrant=((direction+1)/2)%4;
            String clip=new String[]{"forward","right","back","left"}[quadrant];
            V3 travel=V3.look(view+direction*45,0);
            float body=DashVisuals.yaw(P+"dash_air_"+clip,travel,view);
            assertEquals(1,V3.look(body+quadrant*90,0).dot(travel),1e-6);
        }
    }
    @Test void alreadyCorrectCardinalAirDashesKeepTheirBodyHeading(){
        String[] directions={"forward","right","back","left"};
        for(float view:new float[]{-361,-70,0,42,180,500})for(int i=0;i<4;i++){
            float model=DashVisuals.yaw(P+"dash_air_"+directions[i],V3.look(view+i*90,0),view);
            assertEquals(1,V3.look(model,0).dot(V3.look(view,0)),1e-6);
        }
    }
    @Test void otherActionsAndEmptyDirectionsKeepNormalFacing(){
        assertEquals(17,DashVisuals.yaw(P+"weapon_run_n",V3.look(90,0),17));
        assertEquals(17,DashVisuals.yaw(P+"dash_ground",V3.ZERO,17));
        for(String direction:new String[]{"forward","right","back","left"})
            assertTrue(SkinnedAsset.shared().clips.containsKey(AnimationSource.movementClip(false,"dash_air_"+direction)));
        assertTrue(SkinnedAsset.shared().clips.containsKey(P+"dash_ground"));
    }
}
