package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class LocalTextClipTest {
    private static final List<LocalTextClip.Vertex> QUAD=List.of(v(0,0,0,0),v(0,20,0,1),v(40,20,1,1),v(40,0,1,0));
    private static LocalTextClip.Vertex v(float x,float y,float u,float uv){return new LocalTextClip.Vertex(x,y,0,u,uv);}
    @Test void clipsGeometryAndTextureAtTheSameFraction(){
        var result=LocalTextClip.right(QUAD,10);
        assertEquals(4,result.size());assertEquals(200,area(result),1e-5);
        for(var vertex:result){assertTrue(vertex.x()<=10);assertEquals(vertex.x()/40,vertex.u(),1e-6);assertEquals(vertex.y()/20,vertex.v(),1e-6);}
    }
    @Test void filledAndEmptyEndpointsPreserveTheStencil(){
        assertEquals(QUAD,LocalTextClip.right(QUAD,50));assertTrue(LocalTextClip.right(QUAD,-1).isEmpty());
        assertEquals(-16,LocalTextClip.cutoff(0,40,16,0));assertEquals(56,LocalTextClip.cutoff(0,40,16,1));
        assertEquals(20,LocalTextClip.cutoff(0,40,16,.5F));
    }
    @Test void revealBoundaryRotatesWithTheLabelAtEveryGuiScale(){
        var result=LocalTextClip.right(QUAD,10);
        for(float scale:new float[]{.25F,.5F,1,1.5F,2})for(float degrees:new float[]{-10,0,25}){
            Matrix3x2f pose=new Matrix3x2f().translate(430,700).scale(scale).rotate((float)Math.toRadians(degrees));
            var edge=result.stream().filter(p->p.x()==10).map(p->pose.transformPosition(p.x(),p.y(),new Vector2f())).toList();
            assertEquals(2,edge.size());Vector2f direction=new Vector2f(edge.get(1)).sub(edge.get(0)).normalize();
            Vector2f expected=pose.transformDirection(0,1,new Vector2f()).normalize();
            assertEquals(1,Math.abs(direction.dot(expected)),1e-5);
            Matrix3x2f inverse=new Matrix3x2f(pose).invert();
            for(var vertex:result){Vector2f local=inverse.transformPosition(pose.transformPosition(vertex.x(),vertex.y(),new Vector2f()));assertTrue(local.x<=10.001);}
        }
    }
    @Test void handlesItalicGlyphWithoutLosingUvAtCutEdges(){
        var quad=List.of(v(5,0,0,0),v(0,20,0,1),v(40,20,1,1),v(45,0,1,0));
        var result=LocalTextClip.right(quad,3);assertEquals(3,result.size());assertEquals(18,area(result),1e-5);
        for(var vertex:result)assertTrue(vertex.x()<=3);
    }
    private static double area(List<LocalTextClip.Vertex> polygon){double area=0;for(int i=0;i<polygon.size();i++){var a=polygon.get(i);var b=polygon.get((i+1)%polygon.size());area+=a.x()*b.y()-b.x()*a.y();}return Math.abs(area)*.5;}
}
