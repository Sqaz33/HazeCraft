package dev.local.haze.client;

import dev.local.haze.sim.*;
import com.mojang.blaze3d.vertex.*;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.lwjgl.system.MemoryUtil;
import java.nio.ByteBuffer;

/** Indexed triangles retain the full retail mesh while avoiding repeated vertex transforms/uploads. */
final class HazeMeshRenderer implements AutoCloseable {
    private final IndexedTopology topology;
    private final ByteBuffer indices;
    private final VertexFormat.IndexType indexType;
    private final ByteBufferBuilder vertices;
    HazeMeshRenderer(SkinnedAsset.Primitive primitive) {
        topology=IndexedTopology.of(primitive.vertices().position().length/3,primitive.indices());
        indexType=VertexFormat.IndexType.least(topology.vertices().length);
        indices=MemoryUtil.memAlloc(topology.indices().length*indexType.bytes);
        for(int index:topology.indices())if(indexType==VertexFormat.IndexType.SHORT)indices.putShort((short)index);else indices.putInt(index);
        indices.flip();
        vertices=new ByteBufferBuilder(topology.vertices().length*DefaultVertexFormat.NEW_ENTITY.getVertexSize()+64);
    }
    void draw(RenderType type,PoseStack.Pose pose,SkinnedAsset.Primitive primitive,SkinnedAsset.Geometry geometry,int light,int alpha) {
        BufferBuilder buffer=new BufferBuilder(vertices,VertexFormat.Mode.TRIANGLES,DefaultVertexFormat.NEW_ENTITY);
        var matrix=pose.pose();var normalMatrix=pose.normal();
        float[] positions=geometry.position(),normals=geometry.normal(),uv=primitive.vertices().uv();
        int color=(alpha<<24)|0xffffff;
        for(int index:topology.vertices()) {
            int i=index*3,u=index*2;
            float x=positions[i],y=positions[i+1],z=positions[i+2],nx=normals[i],ny=normals[i+1],nz=normals[i+2];
            // World actor transform is rigid (translation and yaw), so unit normals stay unit length.
            buffer.addVertex(matrix.m00()*x+matrix.m10()*y+matrix.m20()*z+matrix.m30(),
                matrix.m01()*x+matrix.m11()*y+matrix.m21()*z+matrix.m31(),
                matrix.m02()*x+matrix.m12()*y+matrix.m22()*z+matrix.m32(),color,uv[u],uv[u+1],OverlayTexture.NO_OVERLAY,light,
                normalMatrix.m00()*nx+normalMatrix.m10()*ny+normalMatrix.m20()*nz,
                normalMatrix.m01()*nx+normalMatrix.m11()*ny+normalMatrix.m21()*nz,
                normalMatrix.m02()*nx+normalMatrix.m12()*ny+normalMatrix.m22()*nz);
        }
        MeshData mesh=buffer.buildOrThrow();
        type.draw(new IndexedMesh(mesh,indices,new MeshData.DrawState(DefaultVertexFormat.NEW_ENTITY,
            topology.vertices().length,topology.indices().length,VertexFormat.Mode.TRIANGLES,indexType)));
    }
    private static final class IndexedMesh extends MeshData {
        private final MeshData vertices;
        private final ByteBuffer indices;
        IndexedMesh(MeshData vertices,ByteBuffer indices,DrawState state){super(null,state);this.vertices=vertices;this.indices=indices;}
        @Override public ByteBuffer vertexBuffer(){return vertices.vertexBuffer();}
        @Override public ByteBuffer indexBuffer(){return indices.duplicate();}
        @Override public void close(){vertices.close();}
    }
    @Override public void close(){vertices.close();MemoryUtil.memFree(indices);}
}
