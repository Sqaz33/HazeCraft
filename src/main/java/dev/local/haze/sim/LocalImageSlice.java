package dev.local.haze.sim;

/** Crop a masked image in panel coordinates before applying its parent's rotation. */
public record LocalImageSlice(float x0,float y0,float x1,float y1,float u0,float v0,float u1,float v1) {
    public static LocalImageSlice region(float x,float y,float width,float height,float left,float top,float right,float bottom){
        left=clamp(left);top=clamp(top);right=clamp(right);bottom=clamp(bottom);
        return new LocalImageSlice(x+width*left,y+height*top,x+width*right,y+height*bottom,left,top,right,bottom);
    }
    public static LocalImageSlice bottom(float x,float y,float width,float height,double value,double maximum){
        float fraction=maximum>0&&Double.isFinite(value)?(float)Math.max(0,Math.min(1,value/maximum)):0;
        return region(x,y,width,height,0,1-fraction,1,1);
    }
    public boolean empty(){return x1<=x0||y1<=y0;}
    private static float clamp(float value){return Math.max(0,Math.min(1,value));}
}
