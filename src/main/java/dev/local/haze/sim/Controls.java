package dev.local.haze.sim;

public record Controls(int held, int pressed, float yaw, float pitch) {
    public static final int FORWARD=1, BACK=2, LEFT=4, RIGHT=8, JUMP=16, DASH=32, CROUCH=64,
        FIRE=128, AIM=256, RELOAD=512, DAGGER=1024, SMOKE=2048, FIXATION=4096, FLURRY=8192,
        MELEE=16384, PARRY=32768, DOWN_DASH=65536;
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
