package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import static org.junit.jupiter.api.Assertions.*;

class LocalImageSliceTest {
    // Current retail CSS: border margin 30, health margin 4px 0px 1px 1px,
    // health size 66x212, frame 68x220. Fill must retain this exact coordinate system.
    @Test void emptyAndFullHealthStayAtTheRetailStencilOffsets(){
        assertTrue(LocalImageSlice.bottom(31,34,66,212,0,730).empty());
        var full=LocalImageSlice.bottom(31,34,66,212,730,730);
        assertEquals(new LocalImageSlice(31,34,97,246,0,0,1,1),full);
        assertEquals(full,LocalImageSlice.bottom(31,34,66,212,900,730));
        assertTrue(LocalImageSlice.bottom(31,34,66,212,-1,730).empty());
        assertTrue(LocalImageSlice.bottom(31,34,66,212,0,0).empty());
    }
    @Test void damageRevealsTheExistingTextureInsteadOfStretchingIt(){
        var half=LocalImageSlice.bottom(31,34,66,212,365,730);
        assertEquals(140,half.y0());assertEquals(246,half.y1());
        assertEquals(.5F,half.v0());assertEquals(1,half.v1());
        var fractional=LocalImageSlice.bottom(31,34,66,212,364.5,730);
        assertTrue(fractional.y0()>140&&fractional.y0()<141);
        assertEquals((fractional.y0()-34)/212,fractional.v0(),1e-6);
    }
    @Test void fillEdgeRotatesWithItsFrameAcrossWindowScales(){
        var half=LocalImageSlice.bottom(31,34,66,212,365,730);
        for(float scale:new float[]{.25F,.5F,1,1.5F,2}){
            Matrix3x2f pose=new Matrix3x2f().translate(430,700).scale(scale).rotate((float)Math.toRadians(-20));
            Vector2f left=pose.transformPosition(half.x0(),half.y0(),new Vector2f());
            Vector2f right=pose.transformPosition(half.x1(),half.y0(),new Vector2f());
            assertEquals(-Math.tan(Math.toRadians(20)),(right.y-left.y)/(right.x-left.x),1e-5);
            Vector2f local=new Matrix3x2f(pose).invert().transformPosition(left);
            assertEquals(31,local.x,1e-3);assertEquals(140,local.y,1e-3);
        }
    }
    @Test void scaleMarksUseTheSameMaskCoordinatesIncludingNarrowLowerEnd(){
        var small=LocalImageSlice.region(31,34,66,212,0,.75F,.3F,.75F+3F/212);
        assertEquals(193,small.y0());assertEquals(196,small.y1(),1e-5);
        assertEquals(50.8,small.x1(),1e-5);assertEquals(.3F,small.u1());
        var large=LocalImageSlice.region(31,34,66,212,0,.5F,1,.5F+3F/212);
        assertEquals(97,large.x1());assertEquals(1,large.u1());
        assertEquals((large.y0()-34)/212,large.v0(),1e-6);
    }
}
