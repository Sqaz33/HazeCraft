package dev.local.haze.sim.movement;

import dev.local.haze.sim.V3;
import java.util.*;

/** Host geometry adapter for the guest capsule, support query and contact planes. */
public final class CapsuleMover {
    public record Box(double minX,double minY,double minZ,double maxX,double maxY,double maxZ) {
        public Box expand(double v){return new Box(minX-v,minY-v,minZ-v,maxX+v,maxY+v,maxZ+v);}
        public Box union(Box b){return new Box(Math.min(minX,b.minX),Math.min(minY,b.minY),Math.min(minZ,b.minZ),Math.max(maxX,b.maxX),Math.max(maxY,b.maxY),Math.max(maxZ,b.maxZ));}
    }
    @FunctionalInterface public interface World { List<Box> boxes(Box bounds); }
    public record Shape(double lower,double upper,double radius){
        public Box bounds(V3 p){return new Box(p.x()-radius,p.y()+lower-radius,p.z()-radius,p.x()+radius,p.y()+upper+radius,p.z()+radius);}
    }
    public record Contact(double separation,V3 normal){}
    public record Sweep(double fraction,V3 normal){}
    public record Result(V3 position,V3 velocity,boolean grounded,V3 wallNormal,List<V3> planes,double slope){}
    private final double step,radius,top,wallShell,groundBoxScale,frequency,damping;
    private final int iterations;
    private V3 support=V3.ZERO,spring=V3.ZERO,lastPosition,previousSupport;
    public CapsuleMover(double step,double radius,double top,double wallShell,double groundBoxScale,double frequency,double damping,int iterations){
        if(step<0||radius<=0||top<=0||iterations<1)throw new IllegalArgumentException("Capsule configuration");
        this.step=step;this.radius=radius;this.top=top;this.wallShell=wallShell;this.groundBoxScale=groundBoxScale;this.frequency=frequency;this.damping=damping;this.iterations=iterations;
    }
    public Shape shape(boolean duck){return new Shape(step+radius,Math.max(duck?step:top,step+radius),radius);}
    private static double clamp(double v,double lo,double hi){return Math.max(lo,Math.min(hi,v));}
    public static Contact contact(Shape shape,V3 p,Box b){
        double low=p.y()+shape.lower,high=p.y()+shape.upper;
        double bx=clamp(p.x(),b.minX,b.maxX),bz=clamp(p.z(),b.minZ,b.maxZ),sy,by;
        if(low>b.maxY){sy=low;by=b.maxY;}else if(high<b.minY){sy=high;by=b.minY;}
        else {sy=clamp((b.minY+b.maxY)*.5,low,high);by=sy;}
        V3 difference=new V3(p.x()-bx,sy-by,p.z()-bz);double length=difference.length();
        if(length>1e-12)return new Contact(length-shape.radius,difference.mul(1/length));
        double[] depths={p.x()-b.minX+shape.radius,b.maxX-p.x()+shape.radius,
            high-b.minY+shape.radius,b.maxY-low+shape.radius,p.z()-b.minZ+shape.radius,b.maxZ-p.z()+shape.radius};
        V3[] normals={new V3(-1,0,0),new V3(1,0,0),new V3(0,-1,0),new V3(0,1,0),new V3(0,0,-1),new V3(0,0,1)};
        int best=0;for(int i=1;i<6;i++)if(depths[i]<depths[best])best=i;
        return new Contact(-depths[best],normals[best]);
    }
    public static Sweep sweep(Shape shape,V3 from,V3 delta,Box box){
        double fraction=0;
        for(int i=0;i<32;i++){
            Contact c=contact(shape,from.add(delta.mul(fraction)),box);double closing=-delta.dot(c.normal);
            if(c.separation<1e-7){if(closing>1e-10||c.separation<-1e-7)return new Sweep(fraction,c.normal);return null;}
            if(closing<=1e-12)return null;
            fraction+=c.separation/closing;if(fraction>1)return null;
        }
        throw new IllegalStateException("Capsule sweep failed to converge");
    }
    /** Native final projection does not normalize again after clipping. */
    public static V3 clip(V3 velocity,List<V3> planes){
        float x=(float)velocity.x(),y=(float)velocity.y(),z=(float)velocity.z();
        float speed=(float)Math.sqrt(y*y+z*z+x*x);if(speed==0)return V3.ZERO;
        x/=speed;y/=speed;z/=speed;
        for(V3 plane:planes){float nx=(float)plane.x(),ny=(float)plane.y(),nz=(float)plane.z();float into=ny*y+nz*z+x*nx;
            if(into<0){x-=nx*into;y-=ny*into;z-=nz*into;}}
        return new V3(x*speed,y*speed,z*speed);
    }
    public V3 wallContact(V3 origin,boolean duck,World world){
        Shape shape=shape(duck);V3 total=V3.ZERO;
        for(Box b:world.boxes(shape.bounds(origin).expand(wallShell))){Contact c=contact(shape,origin,b);
            if(c.separation<wallShell&&Math.abs(c.normal.y())<=.707)total=total.add(c.normal.mul(wallShell-c.separation));}
        return total.normalized();
    }
    private V3 ground(V3 origin,boolean grounded,double vertical,List<Box> boxes){
        double half=radius*1.4142135381698608*groundBoxScale,center=origin.y()+step+radius;
        double down=radius+step*(grounded?2.049999952316284:vertical<=0?1.049999952316284:.20000000298023224);
        double bottom=center-half,best=-Double.MAX_VALUE;
        for(Box b:boxes){
            if(origin.x()+half<b.minX||origin.x()-half>b.maxX||origin.z()+half<b.minZ||origin.z()-half>b.maxZ)continue;
            if(b.maxY<=bottom+1e-7&&b.maxY>=bottom-down-1e-7)best=Math.max(best,b.maxY);
        }
        return best==-Double.MAX_VALUE?V3.ZERO:new V3(origin.x(),best,origin.z());
    }
    public Result move(V3 origin,V3 velocity,boolean grounded,boolean duck,double dt,World world){
        return move(origin,velocity,grounded,duck,dt,world,false);
    }
    public Result move(V3 origin,V3 velocity,boolean grounded,boolean duck,double dt,World world,boolean scripted){
        if(lastPosition!=null&&origin.sub(lastPosition).length()>1){velocity=V3.ZERO;grounded=false;}
        if(lastPosition==null||origin.sub(lastPosition).length()>1){support=V3.ZERO;spring=V3.ZERO;previousSupport=null;}
        Shape shape=shape(duck);Box bounds=shape.bounds(origin).union(shape.bounds(origin.add(velocity.mul(dt))));
        // Include the independent box ground sweep below the elevated capsule.
        bounds=bounds.union(new Box(origin.x()-radius,origin.y()-step*2,origin.z()-radius,origin.x()+radius,origin.y()+step+radius,origin.z()+radius));
        List<Box> boxes=world.boxes(bounds.expand(wallShell));
        if(support.equals(V3.ZERO)&&grounded)support=ground(origin,true,velocity.y(),boxes);
        SupportSpring.Result sprung=SupportSpring.integrate(spring,velocity,support,(float)origin.y(),grounded,2,false,
            (float)frequency,(float)damping,(float)Math.pow(400*.0254,2),(float)dt);
        if(!scripted){spring=sprung.springVelocity();velocity=sprung.velocity();}else spring=V3.ZERO;
        Result geometry=resolve(origin,velocity,velocity.add(spring).mul(dt),shape,boxes);
        V3 position=geometry.position();velocity=geometry.velocity();
        support=ground(position,grounded,velocity.y(),boxes);boolean onGround=!support.equals(V3.ZERO);
        double slope=0;
        if(onGround){
            if(previousSupport!=null){V3 difference=support.sub(previousSupport);double length=difference.horizontal();
                if(length>=20*.0254){slope=Math.toDegrees(Math.atan2(difference.y(),length));previousSupport=support;}}
            else previousSupport=support;
        }else previousSupport=null;
        lastPosition=position;
        return new Result(position,velocity,onGround,wallContact(position,duck,world),geometry.planes(),slope);
    }
    public Result resolve(V3 origin,V3 displacement,boolean duck,World world){
        Shape shape=shape(duck);List<Box> boxes=world.boxes(shape.bounds(origin).union(shape.bounds(origin.add(displacement))).expand(wallShell));
        return resolve(origin,displacement,displacement,shape,boxes);
    }
    private Result resolve(V3 origin,V3 velocity,V3 displacement,Shape shape,List<Box> boxes){
        V3 position=origin,remaining=displacement;List<V3> planes=new ArrayList<>();
        for(int iteration=0;iteration<iterations;iteration++){
            Contact overlap=null;
            for(Box b:boxes){Contact c=contact(shape,position,b);if(c.separation<-1e-7&&(overlap==null||c.separation<overlap.separation))overlap=c;}
            if(overlap==null)break;
            position=position.add(overlap.normal.mul(-overlap.separation+1e-7));planes.add(overlap.normal);
        }
        for(int iteration=0;iteration<iterations&&remaining.length()>1e-10;iteration++){
            Sweep first=null;
            for(Box b:boxes){Sweep hit=sweep(shape,position,remaining,b);if(hit!=null&&(first==null||hit.fraction<first.fraction))first=hit;}
            if(first==null){position=position.add(remaining);break;}
            position=position.add(remaining.mul(first.fraction));planes.add(first.normal);
            remaining=remaining.mul(1-first.fraction);double into=remaining.dot(first.normal);
            if(into<0)remaining=remaining.sub(first.normal.mul(into));
        }
        velocity=planes.isEmpty()?velocity:clip(velocity,planes);
        return new Result(position,velocity,false,V3.ZERO,List.copyOf(planes),0);
    }
}
