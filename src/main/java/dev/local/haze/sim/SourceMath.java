package dev.local.haze.sim;

/** Shared scalar rules verified against the owned client, build 25939744. */
public final class SourceMath {
    private SourceMath(){}
    /** Clamped scalar remap, verified against the pinned native math oracle. */
    public static float remapClamped(float value,float low,float high,float outputLow,float outputHigh){
        if(low==high)return value-high<0?outputLow:outputHigh;
        float fraction=Math.max(0,Math.min(1,(value-low)/(high-low)));
        return fraction*(outputHigh-outputLow)+outputLow;
    }
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
