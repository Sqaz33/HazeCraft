package dev.local.haze.sim;

/** Native scalar approach for the standing/crouching camera pose blend. */
public final class CameraHeightBlend {
    private final float strength;
    private float standing=1;
    private double lastClock=Double.NaN;
    public CameraHeightBlend(float strength){this.strength=strength;}
    public static float approach(float current,float target,float strength,float dt){
        float signed=Math.max(strength,Float.MIN_NORMAL)*(target-current>=0?1:-1);
        float remaining=(float)Math.sqrt((target-current)/signed);
        float delta=Math.min(remaining,dt)-remaining;
        return target-delta*delta*signed;
    }
    public void reset(){standing=1;lastClock=Double.NaN;}
    public void advanceTo(double clock,boolean crouched){
        float target=crouched?0:1;
        if(!Double.isFinite(lastClock)){standing=target;lastClock=clock;return;}
        if(clock<=lastClock)return;
        standing=approach(standing,target,strength,(float)(clock-lastClock));lastClock=clock;
    }
    public float standing(){return standing;}
    public float height(float standingHeight,float crouchingHeight){return crouchingHeight+(standingHeight-crouchingHeight)*standing;}
}
