package dev.local.haze.sim.movement;

import dev.local.haze.sim.Controls;
import java.util.Arrays;

/** Keeps short keyboard taps between host polls. Native fractional event timing is deferred. */
public final class MovementKeys {
    private final ButtonState buttons=new ButtonState();
    private final DoublePressTracker doubles;
    public MovementKeys() {
        int[] initial=new int[64];Arrays.fill(initial,Integer.MIN_VALUE);
        doubles=new DoublePressTracker(initial);
    }
    public void event(int bit,boolean down) { buttons.event(Integer.toUnsignedLong(bit),down); }
    public Controls sample(int held,float yaw,float pitch,int command) {
        long difference=buttons.held()^Integer.toUnsignedLong(held);
        for(int bit=0;bit<16;bit++)if((difference&(1L<<bit))!=0)event(1<<bit,(held&(1<<bit))!=0);
        int pressed=(int)buttons.pressed();
        long doublePress=doubles.update(command,buttons,.22f,(float)MovementClock.INTERVAL);
        if((doublePress&Controls.CROUCH)!=0)pressed|=Controls.DOWN_DASH;
        buttons.beginCommand();
        return new Controls(held,pressed,yaw,pitch);
    }
}
