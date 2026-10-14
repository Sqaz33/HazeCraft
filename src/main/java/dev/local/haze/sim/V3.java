package dev.local.haze.sim;

public record V3(double x, double y, double z) {
    public static final V3 ZERO = new V3(0,0,0);
    public V3 add(V3 b) { return new V3(x+b.x,y+b.y,z+b.z); }
    public V3 sub(V3 b) { return new V3(x-b.x,y-b.y,z-b.z); }
    public V3 mul(double f) { return new V3(x*f,y*f,z*f); }
    public double dot(V3 b) { return x*b.x+y*b.y+z*b.z; }
    public double length() { return Math.sqrt(dot(this)); }
    public double horizontal() { return Math.hypot(x,z); }
    public V3 normalized() { double l=length(); return l>1e-10 ? mul(1/l) : ZERO; }
    public V3 flat() { return new V3(x,0,z); }
    public V3 withY(double v) { return new V3(x,v,z); }
    public V3 lerp(V3 b,double t) { return mul(1-t).add(b.mul(t)); }
    public boolean finite() { return Double.isFinite(x) && Double.isFinite(y) && Double.isFinite(z); }
    public static V3 look(double yaw,double pitch) {
        double y=Math.toRadians(yaw),p=Math.toRadians(pitch);
        return new V3(-Math.sin(y)*Math.cos(p),-Math.sin(p),Math.cos(y)*Math.cos(p));
    }
}
