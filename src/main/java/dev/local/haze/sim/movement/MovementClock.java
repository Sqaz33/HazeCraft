package dev.local.haze.sim.movement;

import dev.local.haze.sim.Controls;

/** Splits host time at original command boundaries, retaining the fractional phase. */
public final class MovementClock {
    public static final double INTERVAL=1.0/64;
    private double phase;
    private int pendingJumps;
    @FunctionalInterface public interface Step { void run(double dt,Controls input); }
    public void advance(double duration,Controls input,Step step) {
        if(!Double.isFinite(duration)||duration<=0||duration>.05+1e-10)throw new IllegalArgumentException("Host frame duration");
        double remaining=duration;
        pendingJumps=Math.min(2,pendingJumps+input.jumpPresses());
        int edges=input.pressed()&~Controls.JUMP;
        while(remaining>1e-12) {
            double dt=Math.min(remaining,INTERVAL-phase);
            int pressed=edges;
            if(pendingJumps>0){pressed|=Controls.JUMP;pendingJumps--;}
            step.run(dt,new Controls(input.held(),pressed,input.yaw(),input.pitch()));edges=0;
            remaining-=dt;phase+=dt;
            if(phase>=INTERVAL-1e-12)phase=0;
        }
    }
    public double phase() { return phase; }
    public void clearInputs() { pendingJumps=0; }
}
