package dev.local.haze.sim.movement;

import dev.local.haze.sim.Controls;

/** Splits host time at original command boundaries, retaining the fractional phase. */
public final class MovementClock {
    public static final double INTERVAL=1.0/64;
    private double phase;
    private int pendingJumps;
    private int pendingEdges;
    @FunctionalInterface public interface Step { void run(double dt,Controls input); }
    public void advance(double duration,Controls input,Step step) {
        if(!Double.isFinite(duration)||duration<=0||duration>.05+1e-10)throw new IllegalArgumentException("Host frame duration");
        if(!input.events().isEmpty()){advanceTimed(duration,input,step);return;}
        double remaining=duration;
        pendingJumps=Math.min(2,pendingJumps+input.jumpPresses());
        int edges=(input.pressed()&~Controls.JUMP)|pendingEdges;pendingEdges=0;
        while(remaining>1e-12) {
            double dt=Math.min(remaining,INTERVAL-phase);
            int pressed=edges;
            if(pendingJumps>0){pressed|=Controls.JUMP;pendingJumps--;}
            step.run(dt,new Controls(input.held(),pressed,input.yaw(),input.pitch()));edges=0;
            remaining-=dt;phase+=dt;
            if(phase>=INTERVAL-1e-12)phase=0;
        }
    }
    private void advanceTimed(double duration,Controls input,Step step){
        int captured=0;for(var event:input.events())captured|=event.button();
        int edges=(input.pressed()&~captured&~Controls.JUMP)|pendingEdges,held=input.initialHeld(),index=0;pendingEdges=0;
        int crouchPress=-1;
        if((captured&Controls.CROUCH)!=0&&(edges&Controls.DOWN_DASH)!=0){
            edges&=~Controls.DOWN_DASH;
            for(int i=0;i<input.events().size();i++)if(input.events().get(i).button()==Controls.CROUCH&&input.events().get(i).down())crouchPress=i;
        }
        double cursor=0;
        while(cursor<duration-1e-12){
            while(index<input.events().size()&&input.events().get(index).offset()*duration/.05<=cursor+1e-12){
                var event=input.events().get(index++);
                if(index-1==crouchPress)edges|=Controls.DOWN_DASH;
                if(event.down()){
                    if((held&event.button())==0){if(event.button()==Controls.JUMP)pendingJumps++;else edges|=event.button();}
                    held|=event.button();
                }else held&=~event.button();
            }
            double end=Math.min(duration,cursor+INTERVAL-phase);
            if(index<input.events().size())end=Math.min(end,input.events().get(index).offset()*duration/.05);
            double dt=end-cursor;
            if(dt<=1e-12){cursor=end;continue;}
            int pressed=edges;if(pendingJumps>0){pressed|=Controls.JUMP;pendingJumps--;}
            step.run(dt,new Controls(held,pressed,input.yaw(),input.pitch()));edges=0;
            phase+=dt;if(phase>=INTERVAL-1e-12)phase=0;cursor=end;
        }
        while(index<input.events().size()){
            var event=input.events().get(index++);
            if(index-1==crouchPress)edges|=Controls.DOWN_DASH;
            if(event.down()){
                if((held&event.button())==0){if(event.button()==Controls.JUMP)pendingJumps++;else edges|=event.button();}
                held|=event.button();
            }else held&=~event.button();
        }
        if(edges!=0||pendingJumps>0){
            do {
                int pressed=edges;if(pendingJumps>0){pressed|=Controls.JUMP;pendingJumps--;}
                step.run(0,new Controls(held,pressed,input.yaw(),input.pitch()));edges=0;
            }while(pendingJumps>0);
        }
        pendingEdges=0;
    }
    public double phase() { return phase; }
    public void clearInputs() { pendingJumps=0;pendingEdges=0; }
}
