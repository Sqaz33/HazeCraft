package dev.local.haze.sim.movement;

import dev.local.haze.sim.Controls;
import java.util.Arrays;
import java.util.ArrayList;

/** Keeps the ordered input transitions and their fractional times between host polls. */
public final class MovementKeys {
    private final ButtonState buttons=new ButtonState();
    private final DoublePressTracker doubles;
    private int jumpPresses;
    private record Event(long nanos,int bit,boolean down){}
    private final ArrayList<Event> pending=new ArrayList<>();
    private long sampleNanos;
    private int initialHeld;
    public MovementKeys(){this(System.nanoTime());}
    public MovementKeys(long nanos) {
        sampleNanos=nanos;
        int[] initial=new int[64];Arrays.fill(initial,Integer.MIN_VALUE);
        doubles=new DoublePressTracker(initial);
    }
    public void event(int bit,boolean down) {
        event(bit,down,System.nanoTime());
    }
    public void event(int bit,boolean down,long nanos) {
        if(((buttons.held()&Integer.toUnsignedLong(bit))!=0)==down)return;
        pending.add(new Event(nanos,bit,down));
        if(bit==Controls.JUMP&&down)jumpPresses++;
        buttons.event(Integer.toUnsignedLong(bit),down);
    }
    public Controls sample(int held,float yaw,float pitch,int command) {
        return sample(held,yaw,pitch,command,System.nanoTime());
    }
    public Controls sample(int held,float yaw,float pitch,int command,long nowNanos) {
        long difference=buttons.held()^Integer.toUnsignedLong(held);
        long reconciliation=pending.isEmpty()?sampleNanos:nowNanos;
        for(int bit=0;bit<16;bit++)if((difference&(1L<<bit))!=0)event(1<<bit,(held&(1<<bit))!=0,reconciliation);
        int pressed=(int)buttons.pressed();
        long doublePress=doubles.update(command,buttons,.22f,(float)MovementClock.INTERVAL);
        if((doublePress&Controls.CROUCH)!=0)pressed|=Controls.DOWN_DASH;
        int jumps=Math.max(jumpPresses,(pressed&Controls.JUMP)!=0?1:0);jumpPresses=0;
        var events=new ArrayList<Controls.TimedButton>();long span=Math.max(1,nowNanos-sampleNanos);
        for(Event event:pending)events.add(new Controls.TimedButton(Math.clamp((double)(event.nanos-sampleNanos)/span,0,1)*.05,event.bit,event.down));
        int before=initialHeld;initialHeld=held;pending.clear();sampleNanos=nowNanos;
        buttons.beginCommand();
        return new Controls(held,pressed,yaw,pitch,jumps,before,events);
    }
}
