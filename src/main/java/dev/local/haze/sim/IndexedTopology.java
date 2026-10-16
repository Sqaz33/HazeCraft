package dev.local.haze.sim;

import java.util.Arrays;

/** One compact vertex per original index; triangle order and material boundaries are unchanged. */
public record IndexedTopology(int[] vertices,int[] indices) {
    public static IndexedTopology of(int vertexCount,int[] source) {
        int[] remap=new int[vertexCount],vertices=new int[vertexCount],indices=new int[source.length];
        Arrays.fill(remap,-1);int count=0;
        for(int i=0;i<source.length;i++) {
            int original=source[i];
            if(original<0||original>=vertexCount)throw new IllegalArgumentException("Mesh index outside vertex stream");
            if(remap[original]<0){remap[original]=count;vertices[count++]=original;}
            indices[i]=remap[original];
        }
        return new IndexedTopology(Arrays.copyOf(vertices,count),indices);
    }
}
