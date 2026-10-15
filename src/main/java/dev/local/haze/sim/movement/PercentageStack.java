package dev.local.haze.sim.movement;

/** Native common percentage aggregation: separate products for bonuses and penalties. */
public final class PercentageStack {
    private float positive=1,negative=1;
    public void add(float percentage) {
        if(!Float.isFinite(percentage))throw new IllegalArgumentException("Non-finite percentage");
        float fraction=percentage/100;
        if(percentage>=0)positive*=(1-fraction);
        else negative*=(1+fraction);
    }
    public float positiveProduct() { return positive; }
    public float negativeProduct() { return negative; }
    public float multiplier() { return Math.max(0,(negative+1)-positive); }
}
