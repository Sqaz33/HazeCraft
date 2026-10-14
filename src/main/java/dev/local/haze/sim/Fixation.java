package dev.local.haze.sim;

/** Per-caster, per-target stack lifetime. Host stores this on its server. */
public final class Fixation {
    public int stacks;
    public double expires, sleepUntil, minimumSleepUntil, wakeAt=Double.POSITIVE_INFINITY,
        bulletResistUntil, slowUntil, resistReduction, postSleepUntil;
    private boolean postSleepPending;
    private int sleepingRank;
    public boolean asleep(double now) { return now<sleepUntil && now<wakeAt; }
    public void expire(double now) { if (now>=expires) stacks=0; }
    public record Hit(double extraBulletDamage,double spiritProc,int addedStacks) {}
    public Hit bullet(Retail data,int rank,boolean headshot,double now) {
        expire(now);
        int before=stacks;
        int add=headshot?(int)data.prop("ability_stacking_damage","HeadshotStacks",rank):1;
        stacks=Math.min((int)data.prop("ability_stacking_damage","MaxStacks",rank),stacks+add);
        expires=now+data.prop("ability_stacking_damage","AbilityDuration",rank);
        double extra=stacks*data.prop("ability_stacking_damage","DamageBonusFixedPerStack",rank);
        int step=(int)data.prop("ability_stacking_damage","ProcDamageStackCount",rank);
        double proc=step>0 && stacks/step>before/step ? data.prop("ability_stacking_damage","ProcDamage",rank) : 0;
        if (proc>0) slowUntil=now+data.prop("ability_stacking_damage","SlowDuration",rank);
        return new Hit(extra,proc,stacks-before);
    }
    public void dagger(Retail data,int rank,double now) { dagger(data,rank,0,now); }
    public void dagger(Retail data,int rank,int fixationRank,double now) {
        sleepUntil=now+data.prop("ability_sleep_dagger","SleepDuration",rank);
        minimumSleepUntil=now+data.prop("ability_sleep_dagger","MinimumSleepTime",rank);
        wakeAt=Double.POSITIVE_INFINITY;
        expire(now);
        int added=(int)data.prop("ability_sleep_dagger","FixationStacks",rank);
        stacks=Math.min((int)data.prop("ability_stacking_damage","MaxStacks",fixationRank),stacks+added);
        if(added>0) expires=now+data.prop("ability_stacking_damage","AbilityDuration",fixationRank);
        sleepingRank=rank;postSleepPending=true;
    }
    public void damaged(Retail data,int daggerRank,double now) {
        if (asleep(now)) wakeAt=Math.min(wakeAt,Math.max(minimumSleepUntil,now+data.prop("ability_sleep_dagger","SleepWakeUpDelay",daggerRank)));
    }
    /** Retail m_PostSleep* modifiers start when sleep expires or damage wakes it. */
    public void advance(Retail data,double now) {
        expire(now);
        if(postSleepPending&&!asleep(now)) {
            double woke=Math.min(sleepUntil,wakeAt);
            bulletResistUntil=woke+data.prop("ability_sleep_dagger","BulletResistReductionDuration",sleepingRank);
            resistReduction=data.prop("ability_sleep_dagger","BulletResistReduction",sleepingRank);
            postSleepUntil=woke+data.prop("ability_sleep_dagger","DebuffDuration",sleepingRank);
            postSleepPending=false;
        }
    }
    public double movementSlow(Retail data,int fixationRank,double now) {
        return Math.max(now<slowUntil?data.prop("ability_stacking_damage","SlowPercent",fixationRank):0,
            now<postSleepUntil?data.prop("ability_sleep_dagger","SlowPercent",sleepingRank):0);
    }
    public boolean live(double now) { return stacks>0||asleep(now)||now<slowUntil||now<postSleepUntil||now<bulletResistUntil; }
}
