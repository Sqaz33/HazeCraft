package dev.local.haze.sim.movement;

/** Command-number history, independent of character, key binding and host tick rate. */
public final class DoublePressTracker {
    private final int[] lastPress;
    public DoublePressTracker(int[] initialLastPress) {
        if(initialLastPress.length!=64)throw new IllegalArgumentException("64 button timestamps required");
        lastPress=initialLastPress.clone();
    }
    public long update(int commandNumber,ButtonState buttons,float windowSeconds,float tickInterval) {
        if(!Float.isFinite(windowSeconds) || windowSeconds<0 || !Float.isFinite(tickInterval) || tickInterval<=0)
            throw new IllegalArgumentException("Invalid double-press timing");
        int window=(int)(windowSeconds/tickInterval+.5f);
        long pressed=buttons.pressed(),within=buttons.doublePressedWithinCommand(),result=0;
        for(int bit=0;bit<64;bit++) {
            long mask=1L<<bit;
            if((pressed&mask)!=0) {
                if((within&mask)!=0 || Integer.compareUnsigned(commandNumber-lastPress[bit],window)<0)
                    result|=mask;
                lastPress[bit]=commandNumber;
            } else if((within&mask)!=0) {
                result|=mask;
                lastPress[bit]=commandNumber;
            }
        }
        return result;
    }
    public int lastPress(int bit) { return lastPress[bit]; }
}
