package dev.local.haze.sim;

/** Scalar path decoded from the owned client.dll, 0x1821e8ad0. */
public final class SourceMath {
    private SourceMath(){}
    public static float bias(float value,float bias){
        float x=Math.max(0,Math.min(1,value)),b=Math.max(Float.MIN_NORMAL,Math.min(1,bias));
        return x/((1/b-2)*(1-x)+1);
    }
    /** CCitadel jump fatigue: remap [0,recharge] -> [minimum,1], then native Bias. */
    public static double wallJumpPower(Retail data,double elapsed){
        float recharge=(float)data.field("citadel_ability_jump","m_flWallJumpFullPowerRechargeTime");
        float minimum=(float)data.field("citadel_ability_jump","m_flWallJumpPowerMin");
        float fraction=Math.max(0,Math.min(1,(float)elapsed/recharge));
        return bias(minimum+(1-minimum)*fraction,(float)data.field("citadel_ability_jump","m_flWallJumpPowerBias"));
    }
}
