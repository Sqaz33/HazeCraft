package dev.local.haze.sim;

public final class Abilities {
    private final Retail data;
    public final int[] ranks=new int[4];
    public final double[] readyAt=new double[4];
    public double time, smokeStart=-100, smokeUntil, buffUntil, flurryStart, flurryUntil, daggerAt=-1;
    public int smokeCharges=1;
    private double smokeNextRecharge;
    public Abilities(Retail data) { this.data=data; }
    public double p(int index,String name) { return data.prop(Retail.SIGNATURES[index],name,ranks[index]); }
    public boolean upgrade(int index) {
        if(index<0 || index>=4 || ranks[index]>=3) return false;
        ranks[index]++;
        if(index==1 && ranks[index]==2) smokeCharges=Math.max(smokeCharges,2);
        return true;
    }
    public boolean cast(int index) {
        if(index<0 || index>=4 || index==2 || time<readyAt[index] || flurryUntil>time) return false;
        if(index==1) {
            if(smokeCharges<=0) return false;
            smokeCharges--; smokeStart=time;
            smokeUntil=time+p(1,"AbilityDuration");
            if(smokeNextRecharge<=time) smokeNextRecharge=time+p(1,"AbilityCooldown");
            readyAt[1]=time+(ranks[1]>=2?p(1,"AbilityCooldownBetweenCharge"):p(1,"AbilityCooldown"));
        } else {
            readyAt[index]=time+p(index,"AbilityCooldown");
            if(index==0) daggerAt=time+p(0,"AbilityCastDelay");
            if(index==3) { breakSmoke(); flurryStart=time+p(3,"AbilityCastDelay"); flurryUntil=flurryStart+p(3,"AbilityDuration"); }
        }
        return true;
    }
    public void advance(double dt) {
        time+=dt;
        int max=ranks[1]>=2?(int)p(1,"AbilityCharges"):1;
        if(smokeCharges<max && time>=smokeNextRecharge && smokeNextRecharge>0) {
            smokeCharges++; smokeNextRecharge=time+p(1,"AbilityCooldown");
        }
        if(smokeUntil>0 && time>=smokeUntil) breakSmoke();
    }
    public boolean smokeActive() { return smokeUntil>time; }
    public void debugRank(int index,int rank){
        if(index<0||index>=4)return;
        ranks[index]=Math.max(0,Math.min(3,rank));clearCooldowns();
    }
    public void debugStop(){
        smokeUntil=buffUntil=flurryUntil=0;daggerAt=-1;
    }
    /** Debug refill preserves cast delays and running effects. */
    public void clearCooldowns() {
        java.util.Arrays.fill(readyAt,time);
        smokeCharges=ranks[1]>=2?(int)p(1,"AbilityCharges"):1;
        smokeNextRecharge=0;
    }
    public boolean invisible() { return smokeActive() && time-smokeStart>=p(1,"InvisFadeToDuration"); }
    public double cooldown(int index){return Math.max(0,Math.max(readyAt[index],index==1&&smokeCharges<=0?smokeNextRecharge:0)-time);}
    public boolean flurry() { return time>=flurryStart && time<flurryUntil; }
    public void breakSmoke() {
        if(smokeUntil>0) buffUntil=time+p(1,"PostInvisBuffDuration");
        smokeUntil=0;
    }
    public boolean consumeDagger() {
        if(daggerAt>=0 && time>=daggerAt) { daggerAt=-1;return true; }return false;
    }
}
