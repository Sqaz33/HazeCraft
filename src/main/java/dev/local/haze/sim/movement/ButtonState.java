package dev.local.haze.sim.movement;

/** Shared command button state. A short press/release remains observable in the command. */
public final class ButtonState {
    private static final int[] EVENT={0,3,2,1,2,5,4,3,4,7,6,5,6,5,4,7};
    private long held,changed,history;
    public ButtonState() {}
    public ButtonState(long held,long changed,long history) {
        this.held=held;this.changed=changed;this.history=history;
    }
    public void event(long button,boolean down) {
        if(Long.bitCount(button)!=1)throw new IllegalArgumentException("One button per input event");
        int state=encoding(button),next=EVENT[state*2+(down?1:0)];
        held=(held&~button)|((next&1)!=0?button:0);
        changed=(changed&~button)|((next&2)!=0?button:0);
        history=(history&~button)|((next&4)!=0?button:0);
    }
    public int encoding(long button) {
        return ((held&button)!=0?1:0)|((changed&button)!=0?2:0)|((history&button)!=0?4:0);
    }
    public long held() { return held; }
    public long pressed() { return (held&changed)|history; }
    public long released() { return (~held&changed)|history; }
    public long doublePressedWithinCommand() { return held&changed&history; }
    public void beginCommand() { changed=0;history=0; }
}
