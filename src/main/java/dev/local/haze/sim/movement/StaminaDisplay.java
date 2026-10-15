package dev.local.haze.sim.movement;

/** Charge radial geometry recovered from the current native HUD layout. */
public final class StaminaDisplay {
    public record Charge(double start,double extent,double fill) {}
    public static Charge[] charges(double stamina,int count) {
        if(count<1||count>64)throw new IllegalArgumentException("Stamina charge count");
        double gap=count<=4?3:2,width=(90-(count-1)*gap)/count;
        Charge[] result=new Charge[count];
        for(int i=0;i<count;i++)result[i]=new Charge(135+(width+gap)*i,-width,Math.clamp(stamina-i,0,1));
        return result;
    }
}
