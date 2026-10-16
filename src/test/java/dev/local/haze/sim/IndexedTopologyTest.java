package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IndexedTopologyTest {
    @Test void preservesEveryRetailTriangleAndMaterialBoundary(){
        var asset=SkinnedAsset.shared();int oldCount=0,newCount=0;
        for(var primitive:asset.primitives){
            var topology=IndexedTopology.of(primitive.vertices().position().length/3,primitive.indices());
            assertEquals(primitive.indices().length,topology.indices().length);
            for(int i=0;i<topology.indices().length;i++)assertEquals(primitive.indices()[i],topology.vertices()[topology.indices()[i]]);
            oldCount+=primitive.indices().length;newCount+=topology.vertices().length;
        }
        assertTrue(newCount<oldCount/5,"Expected full retail mesh indexing to remove over 80% of vertex submissions");
        System.out.println("Haze indexed vertex submissions: "+oldCount+" -> "+newCount);
    }
    @Test void respectsSplitVerticesAndRepeatedWinding(){
        var topology=IndexedTopology.of(8,new int[]{6,2,1,1,2,6,7,6,2});
        assertArrayEquals(new int[]{6,2,1,7},topology.vertices());
        assertArrayEquals(new int[]{0,1,2,2,1,0,3,0,1},topology.indices());
        assertThrows(IllegalArgumentException.class,()->IndexedTopology.of(2,new int[]{2}));
    }
}
