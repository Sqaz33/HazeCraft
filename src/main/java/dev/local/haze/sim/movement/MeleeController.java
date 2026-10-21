package dev.local.haze.sim.movement;

import com.google.gson.*;
import dev.local.haze.sim.*;

/** Shared predicted/authoritative phase machine, parameterized by the character's melee role. */
public final class MeleeController {
    public enum Phase { NONE, CHARGING, GROUND_DASH, AIR_DASH, ATTACKING, SLIDE_DASH }
    public enum Type { NONE, LIGHT, HEAVY, HEAVY_AIR, SLIDE }
    private final JsonObject definition,attacks,turnCurve;
    public Phase phase=Phase.NONE;
    public Type type=Type.NONE;
    public double itemDistanceScale;
    public double time,phaseStart=-100,chargeStart=-100,strikeTime=-100,readyAt;
    public long serial,strikeSerial;
    public float yaw,pitch;
    public V3 direction=V3.ZERO;
    private boolean forceHeavy;
    public MeleeController(CharacterMovementProfile profile){
        definition=profile.definition(CharacterMovementProfile.Role.MELEE);
        attacks=definition.getAsJsonObject("m_mapAttacks");turnCurve=definition.getAsJsonObject("m_HeavyTurnSpeedCurve");
    }
    private double field(String key){return Retail.number(definition.get(key));}
    public JsonObject attack(){return attacks.getAsJsonObject(switch(type){case LIGHT->"EAttackType_Light";case HEAVY_AIR->"EAttackType_HeavyAir";case SLIDE->"EAttackType_Slide";default->"EAttackType_Heavy";});}
    public double age(){return Math.max(0,time-phaseStart);}
    public double chargeAge(){return Math.max(0,time-chargeStart);}
    public double cooldown(){return Math.max(0,readyAt-time);}
    public double chargeDelay(){return field("m_flChargeAnimDelayTime");}
    public double heavyHold(){return field("m_flHeavyAttackRequiredHoldTime");}
    public double movementDuration(){return attack().getAsJsonObject("m_MovementSpeedCurve").getAsJsonArray("m_vDomainMaxs").get(0).getAsDouble();}
    public boolean active(){return phase!=Phase.NONE;}
    public boolean heavy(){return type!=Type.NONE&&type!=Type.LIGHT;}
    public boolean dashing(){return phase==Phase.GROUND_DASH||phase==Phase.AIR_DASH||phase==Phase.SLIDE_DASH;}
    public boolean locksMovement(){return dashing()||phase==Phase.ATTACKING&&heavy();}
    public void step(double dt,Controls input,boolean grounded,boolean sliding,boolean allowed){
        time+=dt;
        if(!allowed&&active()){cancel();return;}
        if(phase==Phase.NONE&&allowed&&time>=readyAt&&(input.down(Controls.MELEE)||input.edge(Controls.MELEE))){
            phase=Phase.CHARGING;phaseStart=chargeStart=time-dt;forceHeavy=sliding;type=Type.NONE;serial++;
        }
        if(phase==Phase.CHARGING){
            yaw=input.yaw();pitch=input.pitch();
            if(!forceHeavy&&!input.down(Controls.MELEE)&&chargeAge()<=field("m_flLightAttackMaxHoldTime")){
                type=Type.LIGHT;direction=V3.look(yaw,0);strike();
            }else if(chargeAge()>=heavyHold()){
                type=!grounded?Type.HEAVY_AIR:sliding?Type.SLIDE:Type.HEAVY;
                phase=!grounded?Phase.AIR_DASH:sliding?Phase.SLIDE_DASH:Phase.GROUND_DASH;
                phaseStart=time;direction=V3.look(yaw,phase==Phase.AIR_DASH?pitch:0);
            }
        }else if(dashing()){
            if(phase==Phase.AIR_DASH&&grounded){phase=Phase.GROUND_DASH;direction=direction.flat().normalized();}
            if(phase!=Phase.AIR_DASH){
                double requested=input.yaw(),difference=Math.IEEEremainder(requested-yaw,360);
                double rate=field("m_flHeavyMeleeMaxTurnRate")*Curve.sample(turnCurve,age()/movementDuration());
                yaw+=(float)Math.clamp(difference,-rate*dt,rate*dt);direction=V3.look(yaw,0);
            }
            if(age()>movementDuration())strike();
        }else if(phase==Phase.ATTACKING&&age()>Retail.number(attack().get("m_flAttackStateTime"))){phase=Phase.NONE;phaseStart=time;}
    }
    public void strike(){
        if(phase==Phase.ATTACKING||phase==Phase.NONE)return;
        phase=Phase.ATTACKING;phaseStart=strikeTime=time;strikeSerial++;readyAt=time+Retail.number(attack().get("m_flCooldownOnMiss"));
    }
    public void confirmedHit(boolean hit){readyAt=strikeTime+Retail.number(attack().get(hit?"m_flCooldownOnHit":"m_flCooldownOnMiss"));}
    public void syncCooldown(double remaining){readyAt=time+Math.max(0,remaining);}
    public void cancel(){phase=Phase.NONE;phaseStart=time;}
    public V3 movement(V3 previous,double dt){
        if(!dashing())return phase==Phase.ATTACKING&&heavy()?V3.ZERO:previous;
        double speed=Curve.sample(attack().getAsJsonObject("m_MovementSpeedCurve"),age())*Retail.SOURCE_UNIT*(1+itemDistanceScale/100);
        V3 target=direction.mul(speed);
        if(phase==Phase.AIR_DASH){
            var scale=definition.getAsJsonArray("m_AirMeleeUpScale");
            double factor=SourceMath.remapClamped(pitch,scale.get(0).getAsFloat(),scale.get(1).getAsFloat(),scale.get(2).getAsFloat(),scale.get(3).getAsFloat());
            target=target.withY(target.y()*factor);
        }
        V3 difference=target.sub(previous);double max=Retail.number(attack().get("m_flMovementAcc"))*Retail.SOURCE_UNIT*dt;
        return difference.length()<=max?target:previous.add(difference.normalized().mul(max));
    }
    public double chargeMoveSpeed(){return Retail.number(definition.getAsJsonObject("m_mapAbilityProperties").getAsJsonObject("ChannelMoveSpeed").get("m_strValue"));}
    public double speedBonus(){return type!=Type.NONE&&attack().has("m_SpeedBonusCurve")?Curve.sample(attack().getAsJsonObject("m_SpeedBonusCurve"),time-strikeTime)*Retail.SOURCE_UNIT:0;}
    public double knockUp(){return attack().has("m_flKnockUpStrength")?Retail.number(attack().get("m_flKnockUpStrength"))*Retail.SOURCE_UNIT:0;}
}
