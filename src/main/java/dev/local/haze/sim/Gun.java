package dev.local.haze.sim;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.SplittableRandom;

/** Continuous-time weapon schedule, independent of Minecraft's 20 Hz tick. */
public final class Gun {
    private final Retail data;
    private final JsonObject w;
    public int ammo;
    public double reloadRemaining, bloom, recoil, clock, nextShot;
    public long shotCount;
    private final SplittableRandom random;
    public record Shot(double time, double yawOffset, double pitchOffset, double damage) {}
    public Gun(Retail data) {
        this.data=data; w=data.weapon(); ammo=clipSize(0);
        random=new SplittableRandom((long)f("m_nRecoilSeed"));
    }
    public double f(String key) { return Retail.number(w.get(key)); }
    public int clipSize(double spirit) { return (int)Math.floor(f("m_iClipSize")+spirit*0.5); }
    public void reload(double spirit) {
        if (reloadRemaining<=0 && ammo<clipSize(spirit)) reloadRemaining=f("m_reloadDuration");
    }
    public List<Shot> advance(double dt, boolean fire, boolean aim, boolean infiniteClip, boolean sliding, double fireRateBonus, double damageBonus, double spirit) {
        if (dt<=0 || dt>0.1) throw new IllegalArgumentException("Invalid weapon dt");
        double end=clock+dt;
        List<Shot> shots=new ArrayList<>();
        bloom=Math.max(0,bloom-f("m_flShootSpreadPenaltyDecay")*dt);
        recoil=Math.max(0,recoil-f("m_flRecoilRecoverySpeed")*dt);
        if(sliding)reloadRemaining=0;
        if (reloadRemaining>0) {
            reloadRemaining=Math.max(0,reloadRemaining-dt);
            if (reloadRemaining==0) ammo=clipSize(spirit);
            else { clock=end; nextShot=Math.max(nextShot,end); return shots; }
        }
        if (!fire) { nextShot=Math.max(nextShot,end); clock=end; return shots; }
        double cycle=f("m_flCycleTime")/(1+fireRateBonus/100);
        while (nextShot<end-1e-9) {
            if (!infiniteClip && !sliding && ammo<=0) { reload(spirit); nextShot=end; break; }
            double standing=f("m_StandingSpread");
            double aimingBloom=aim ? w.getAsJsonArray("m_AimingShootSpreadPenalty").get(1).getAsDouble() : 1;
            double spread=(standing+bloom)*aimingBloom;
            double radius=Math.sqrt(random.nextDouble())*spread,angle=random.nextDouble()*Math.PI*2;
            double yaw=Math.cos(angle)*radius,pitch=Math.sin(angle)*radius;
            double kick=f("m_flVerticalPunch")*(sliding?0.5:1);
            shots.add(new Shot(nextShot,yaw,pitch,f("m_flBulletDamage")+damageBonus));
            if (!infiniteClip && !sliding) ammo--;
            bloom+=f("m_flShootSpreadPenaltyPerShot"); recoil+=kick; shotCount++;
            nextShot+=cycle;
        }
        clock=end;
        return shots;
    }
    public double damageAt(double meters) {
        double start=f("m_flDamageFalloffStartRange")*Retail.SOURCE_UNIT,end=f("m_flDamageFalloffEndRange")*Retail.SOURCE_UNIT;
        double t=Math.max(0,Math.min(1,(meters-start)/(end-start)));
        return f("m_flDamageFalloffStartScale")+(f("m_flDamageFalloffEndScale")-f("m_flDamageFalloffStartScale"))*t;
    }
}
