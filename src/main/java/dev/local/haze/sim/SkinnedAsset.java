package dev.local.haze.sim;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.GZIPInputStream;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Local converted mesh and every original clip. No animation keyframe reduction. */
public final class SkinnedAsset {
    public record Node(String name,int parent,Vector3f position,Quaternionf rotation,Vector3f scale,Matrix4f matrix) {}
    public record Skin(int[] joints,Matrix4f[] inverseBind) {}
    public record Vertices(float[] position,float[] normal,float[] uv,int[] joints,float[] weights) {}
    public record Primitive(String texture,int skin,int node,Vertices vertices,int[] indices) {}
    public record Track(int node,int path,float[] times,float[] values,int components,boolean step) {}
    public record Clip(String name,Track[] tracks,float duration) {}
    public record Geometry(float[] position,float[] normal) {}
    public record Layer(String clip,double time,boolean loop,float weight,Map<String,Float> mask,boolean additive,String secondClip,double secondTime,float blend) {
        public Layer(String clip,double time,boolean loop,float weight,Map<String,Float> mask,boolean additive){this(clip,time,loop,weight,mask,additive,"",0,0);}
    }
    public final Node[] nodes;
    public final Skin[] skins;
    public final Primitive[] primitives;
    public final Map<String,Clip> clips;
    public SkinnedAsset(InputStream input) throws IOException {
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new GZIPInputStream(input)))) {
            if(in.readInt()!=0x485a4d31) throw new IOException("Invalid local HZM version");
            nodes=new Node[count(in,512)];
            for(int i=0;i<nodes.length;i++) nodes[i]=new Node(string(in),in.readInt(),vec(in),quat(in),vec(in),new Matrix4f().set(floats(in,16)));
            skins=new Skin[count(in,16)];
            for(int i=0;i<skins.length;i++) {
                int[] joints=ints(in,count(in,512));Matrix4f[] bind=new Matrix4f[joints.length];
                for(int j=0;j<bind.length;j++) bind[j]=new Matrix4f().set(floats(in,16));
                skins[i]=new Skin(joints,bind);
            }
            primitives=new Primitive[count(in,64)];Map<Integer,Vertices> unique=new HashMap<>();
            for(int i=0;i<primitives.length;i++) {
                String texture=string(in);int skin=in.readInt(),node=in.readInt(),vertices=count(in,500000),indices=count(in,2000000);
                Vertices v=new Vertices(floats(in,vertices*3),floats(in,vertices*3),floats(in,vertices*2),ints(in,vertices*4),floats(in,vertices*4));
                Vertices previous=unique.putIfAbsent(node,v);if(previous!=null) v=previous;
                primitives[i]=new Primitive(texture,skin,node,v,ints(in,indices));
            }
            Map<String,Clip> loaded=new LinkedHashMap<>();int count=count(in,10000);
            for(int i=0;i<count;i++) {
                String name=string(in);Track[] tracks=new Track[count(in,5000)];float duration=0;
                for(int j=0;j<tracks.length;j++) {
                    int node=in.readInt(),path=in.readInt(),samples=count(in,100000),components=in.readInt();boolean step=in.readInt()!=0;
                    float[] times=floats(in,samples),values=floats(in,samples*components);
                    tracks[j]=new Track(node,path,times,values,components,step);
                    duration=Math.max(duration,times[times.length-1]);
                }
                loaded.put(name,new Clip(name,tracks,duration));
            }
            if(in.read()!=-1) throw new IOException("Unexpected trailing HZM data");
            clips=Collections.unmodifiableMap(loaded);
        }
    }
    public static SkinnedAsset load() {
        try(InputStream in=SkinnedAsset.class.getResourceAsStream("/assets/haze/retail/haze.hzm.gz")) {
            if(in==null) throw new IOException("Missing converted retail Haze asset");return new SkinnedAsset(in);
        } catch(IOException e) { throw new UncheckedIOException(e); }
    }
    private static final class Shared { private static final SkinnedAsset INSTANCE=load(); }
    public static SkinnedAsset shared(){return Shared.INSTANCE;}
    public V3 attachment(Matrix4f[] pose,String name){
        var attachment=Retail.DATA.root.getAsJsonObject("attachments").getAsJsonObject(name);
        if(attachment==null)throw new IllegalArgumentException("Unrecovered original attachment: "+name);
        var names=attachment.getAsJsonArray("m_influenceNames");var offsets=attachment.getAsJsonArray("m_vInfluenceOffsets");var weights=attachment.getAsJsonArray("m_influenceWeights");
        Vector3f position=new Vector3f();
        for(int influence=0;influence<attachment.get("m_nInfluences").getAsInt();influence++){
            String bone=names.get(influence).getAsString();int index=-1;for(int i=0;i<nodes.length;i++)if(nodes[i].name.equals(bone)){index=i;break;}
            if(index<0)throw new IllegalArgumentException("Missing attachment bone: "+bone);
            var offset=offsets.get(influence).getAsJsonArray();Vector3f local=new Vector3f(offset.get(0).getAsFloat(),offset.get(1).getAsFloat(),offset.get(2).getAsFloat()).mul((float)Retail.SOURCE_UNIT);
            pose[index].transformPosition(local);position.fma(weights.get(influence).getAsFloat(),local);
        }
        return new V3(position.x,position.y,position.z);
    }
    public static V3 worldOffset(V3 local,double yaw){
        double angle=Math.toRadians(-yaw),c=Math.cos(angle),s=Math.sin(angle);
        return new V3(c*local.x()+s*local.z(),local.y(),-s*local.x()+c*local.z());
    }
    public Matrix4f[] pose(String name,double time,boolean loop) {
        return pose(name,time,loop,List.of());
    }
    public Matrix4f[] pose(String name,double time,boolean loop,List<Layer> layers) {
        Clip clip=clips.get(name);if(clip==null) throw new IllegalArgumentException("Unknown original clip: "+name);
        Vector3f[] t=new Vector3f[nodes.length],s=new Vector3f[nodes.length];Quaternionf[] r=new Quaternionf[nodes.length];
        for(int i=0;i<nodes.length;i++) { t[i]=new Vector3f(nodes[i].position);s[i]=new Vector3f(nodes[i].scale);r[i]=new Quaternionf(nodes[i].rotation); }
        float elapsed=clip.duration>0?(float)(loop?time%clip.duration:Math.min(clip.duration,Math.max(0,time))):0;
        float[] sample=new float[4];
        for(Track track:clip.tracks) {
            sample(track,elapsed,sample);
            if(track.path==0) t[track.node].set(sample[0],sample[1],sample[2]);
            else if(track.path==1) r[track.node].set(sample[0],sample[1],sample[2],sample[3]).normalize();
            else s[track.node].set(sample[0],sample[1],sample[2]);
        }
        for(Layer layer:layers) {
            Clip overlay=clips.get(layer.clip);if(overlay==null)continue;
            Clip second=clips.get(layer.secondClip);
            float clock=overlay.duration>0?(float)(layer.loop?layer.time%overlay.duration:Math.min(overlay.duration,Math.max(0,layer.time))):0;
            float[] other=new float[4];
            for(Track track:overlay.tracks) {
                float weight=layer.weight*(layer.mask==null?1:layer.mask.getOrDefault(nodes[track.node].name,0F));
                if(weight<=0)continue;sample(track,clock,sample);
                if(second!=null&&layer.blend>0)for(Track target:second.tracks)if(target.node==track.node&&target.path==track.path){
                    sample(target,(float)layer.secondTime,other);
                    if(track.path==1){Quaternionf q=new Quaternionf(sample[0],sample[1],sample[2],sample[3]).slerp(new Quaternionf(other[0],other[1],other[2],other[3]),layer.blend);sample[0]=q.x;sample[1]=q.y;sample[2]=q.z;sample[3]=q.w;}
                    else for(int k=0;k<track.components;k++)sample[k]+=(other[k]-sample[k])*layer.blend;
                    break;
                }
                if(track.path==0){
                    Vector3f target=new Vector3f(sample[0],sample[1],sample[2]);
                    if(layer.additive)t[track.node].add(target.mul(weight));else t[track.node].lerp(target,weight);
                }else if(track.path==1){
                    Quaternionf target=new Quaternionf(sample[0],sample[1],sample[2],sample[3]).normalize();
                    if(layer.additive)r[track.node].mul(new Quaternionf().slerp(target,weight));else r[track.node].slerp(target,weight);
                }else{
                    Vector3f target=new Vector3f(sample[0],sample[1],sample[2]);
                    if(layer.additive)s[track.node].mul(1+(target.x-1)*weight,1+(target.y-1)*weight,1+(target.z-1)*weight);else s[track.node].lerp(target,weight);
                }
            }
        }
        // Root displacement belongs to the movement simulation, not a second render translation.
        for(int i=0;i<nodes.length;i++) if(nodes[i].name.equals("root_motion")) { t[i].x=nodes[i].position.x;t[i].z=nodes[i].position.z; }
        Matrix4f[] global=new Matrix4f[nodes.length];
        for(int i=0;i<nodes.length;i++) computeGlobal(i,t,r,s,global);
        return global;
    }
    private Matrix4f computeGlobal(int i,Vector3f[] t,Quaternionf[] r,Vector3f[] s,Matrix4f[] global) {
        if(global[i]!=null) return global[i];
        Matrix4f local=new Matrix4f(nodes[i].matrix).translate(t[i]).rotate(r[i]).scale(s[i]);
        return global[i]=nodes[i].parent<0?local:new Matrix4f(computeGlobal(nodes[i].parent,t,r,s,global)).mul(local);
    }
    public Geometry[] skin(Matrix4f[] global) {
        Matrix4f[][] transforms=new Matrix4f[skins.length][];
        for(int i=0;i<skins.length;i++) {
            Skin skin=skins[i];transforms[i]=new Matrix4f[skin.joints.length];
            for(int j=0;j<skin.joints.length;j++) transforms[i][j]=new Matrix4f(global[skin.joints[j]]).mul(skin.inverseBind[j]);
        }
        IdentityHashMap<Vertices,Geometry> done=new IdentityHashMap<>();Geometry[] result=new Geometry[primitives.length];
        for(int p=0;p<primitives.length;p++) {
            Primitive primitive=primitives[p];Vertices v=primitive.vertices;
            Geometry geometry=done.get(v);
            if(geometry==null) {
                float[] pos=new float[v.position.length],normal=new float[v.normal.length];
                Matrix4f[] palette=transforms[primitive.skin];
                for(int i=0;i<pos.length/3;i++) {
                    float x=v.position[i*3],y=v.position[i*3+1],z=v.position[i*3+2];
                    float nx=v.normal[i*3],ny=v.normal[i*3+1],nz=v.normal[i*3+2];
                    float px=0,py=0,pz=0,nnx=0,nny=0,nnz=0;
                    for(int k=0;k<4;k++) {
                        float w=v.weights[i*4+k];if(w==0) continue;Matrix4f m=palette[v.joints[i*4+k]];
                        px+=(m.m00()*x+m.m10()*y+m.m20()*z+m.m30())*w;
                        py+=(m.m01()*x+m.m11()*y+m.m21()*z+m.m31())*w;
                        pz+=(m.m02()*x+m.m12()*y+m.m22()*z+m.m32())*w;
                        nnx+=(m.m00()*nx+m.m10()*ny+m.m20()*nz)*w;
                        nny+=(m.m01()*nx+m.m11()*ny+m.m21()*nz)*w;
                        nnz+=(m.m02()*nx+m.m12()*ny+m.m22()*nz)*w;
                    }
                    pos[i*3]=px;pos[i*3+1]=py;pos[i*3+2]=pz;
                    float len=(float)Math.sqrt(nnx*nnx+nny*nny+nnz*nnz);if(len==0) len=1;
                    normal[i*3]=nnx/len;normal[i*3+1]=nny/len;normal[i*3+2]=nnz/len;
                }
                geometry=new Geometry(pos,normal);done.put(v,geometry);
            }
            result[p]=geometry;
        }
        return result;
    }
    private static void sample(Track t,float time,float[] out) {
        int i=Arrays.binarySearch(t.times,time);
        if(i<0) i=Math.max(0,-i-2);
        int j=Math.min(i+1,t.times.length-1);
        float f=t.step||i==j?0:Math.max(0,Math.min(1,(time-t.times[i])/(t.times[j]-t.times[i])));
        if(t.path==1) {
            int a=i*4,b=j*4;
            Quaternionf q=new Quaternionf(t.values[a],t.values[a+1],t.values[a+2],t.values[a+3]);
            q.slerp(new Quaternionf(t.values[b],t.values[b+1],t.values[b+2],t.values[b+3]),f);
            out[0]=q.x;out[1]=q.y;out[2]=q.z;out[3]=q.w;
        } else for(int k=0;k<t.components;k++) out[k]=t.values[i*t.components+k]*(1-f)+t.values[j*t.components+k]*f;
    }
    private static int count(DataInputStream in,int max)throws IOException { int n=in.readInt();if(n<0||n>max)throw new IOException("HZM count "+n);return n; }
    private static String string(DataInputStream in)throws IOException { return new String(in.readNBytes(count(in,65536)),StandardCharsets.UTF_8); }
    private static Vector3f vec(DataInputStream in)throws IOException { return new Vector3f(in.readFloat(),in.readFloat(),in.readFloat()); }
    private static Quaternionf quat(DataInputStream in)throws IOException { return new Quaternionf(in.readFloat(),in.readFloat(),in.readFloat(),in.readFloat()); }
    private static float[] floats(DataInputStream in,int n)throws IOException { float[] a=new float[n];for(int i=0;i<n;i++)a[i]=in.readFloat();return a; }
    private static int[] ints(DataInputStream in,int n)throws IOException { int[] a=new int[n];for(int i=0;i<n;i++)a[i]=in.readInt();return a; }
}
