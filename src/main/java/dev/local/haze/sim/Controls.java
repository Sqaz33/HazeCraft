package dev.local.haze.sim;

public record Controls(int held, int pressed, float yaw, float pitch, int jumpPresses,int initialHeld,java.util.List<TimedButton> events) {
    public record TimedButton(double offset,int button,boolean down){}
    public Controls { events=java.util.List.copyOf(events); }
    public Controls(int held,int pressed,float yaw,float pitch,int jumpPresses){this(held,pressed,yaw,pitch,jumpPresses,held,java.util.List.of());}
    public Controls(int held,int pressed,float yaw,float pitch) {
        this(held,pressed,yaw,pitch,(pressed&JUMP)!=0?1:0);
    }
    public static final int FORWARD=1, BACK=2, LEFT=4, RIGHT=8, JUMP=16, DASH=32, CROUCH=64,
        FIRE=128, AIM=256, RELOAD=512, DAGGER=1024, SMOKE=2048, FIXATION=4096, FLURRY=8192,
        MELEE=16384, PARRY=32768, DOWN_DASH=65536, SLIDING=131072;
    public static final Controls NONE=new Controls(0,0,0,0);
    public boolean down(int bit) { return (held&bit)!=0; }
    public boolean edge(int bit) { return (pressed&bit)!=0; }
    public Controls withoutEdges() { return new Controls(held,0,yaw,pitch); }
    public V3 wish() {
        double f=(down(FORWARD)?1:0)-(down(BACK)?1:0),s=(down(RIGHT)?1:0)-(down(LEFT)?1:0);
        V3 forward=V3.look(yaw,0),right=V3.look(yaw+90,0);
        return forward.mul(f).add(right.mul(s)).normalized();
    }
}
