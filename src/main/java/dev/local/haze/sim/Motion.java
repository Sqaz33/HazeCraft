package dev.local.haze.sim;

import com.google.gson.*;

/** Guest-space motion. Collision callbacks are the only Minecraft dependency. */
public final class Motion {
    public enum Mode { IDLE, RUN, SPRINT, CROUCH, DASH_GROUND, DASH_AIR, SLIDE, AIR, MANTLE, ROPE, ZIPLINE, MELEE }
    public record MoveResult(V3 actual,boolean ground,boolean wall,V3 wallNormal,double slope) {}
    public interface Collision {
        MoveResult move(V3 displacement,boolean crouch);
        default boolean rope() { return false; }
        default V3 mantle(V3 direction,double maxHeight) { return null; }
        default V3 zipDirection() { return null; }
    }
    private final Retail data;
    public V3 velocity=V3.ZERO;
    public Mode mode=Mode.IDLE;
    public double time,stamina,dashStart=-100,dashDuration,dashDistance,lastGroundTime=-100,
        lastWallJump=-100,slideStart=-100,jumpStart=-100,mantleStart=-100,parryUntil,parryReady,meleeStart=-100,sprintBonus;
    public V3 dashDirection=V3.ZERO,wallNormal=V3.ZERO;
    public boolean grounded=true,wall,airJumpUsed,airDashUsed;
    public int mantleHeight=32;
    private V3 mantleRemaining=V3.ZERO;
    private double mantleLeft,mantleHorizontalTime,zipSpeed;
    private boolean mantleVertical;
    private double slope;
    public Motion(Retail data) { this.data=data;stamina=data.stat("EStamina"); }
    private double f(String id,String key) { return data.field(id,key); }
    private double p(String id,String key) { return data.prop(id,key,0); }
    public void step(double dt,Controls input,Collision world,double bonusSpeed,double capSpeed) {
        step(dt,input,world,bonusSpeed,capSpeed,false);
    }
    public void step(double dt,Controls input,Collision world,double bonusSpeed,double capSpeed,boolean inCombat) {
        if(dt<=0 || dt>0.05) throw new IllegalArgumentException("Motion substep");
        time+=dt;
        stamina=Math.min(data.stat("EStamina"),stamina+data.stat("EStaminaRegenPerSecond")*dt);
        if(grounded) { lastGroundTime=time;airJumpUsed=false;airDashUsed=false; }
        if(input.edge(Controls.PARRY) && time>=parryReady) {
            parryUntil=time+f("citadel_ability_melee_parry","m_flActiveTime");
            parryReady=time+p("citadel_ability_melee_parry","AbilityCooldown");
            if(mode==Mode.DASH_GROUND || mode==Mode.DASH_AIR) {
                velocity=velocity.mul(f("citadel_ability_dash","m_flParryCancelSpeedScale"));
                mode=grounded?Mode.RUN:Mode.AIR;
            }
        }
        V3 wish=input.wish();
        boolean sprint=!inCombat&&wish.length()>0&&!input.down(Controls.CROUCH)&&!input.down(Controls.AIM)&&!input.down(Controls.FIRE)&&!input.down(Controls.MELEE)&&capSpeed<0;
        sprintBonus=sprint?Math.min(data.stat("ESprintSpeed"),sprintBonus+f("citadel_ability_sprint","m_flSprintAccMS")*dt):0;
        if(input.edge(Controls.DASH) && stamina>=1 && (grounded || !airDashUsed) && mode!=Mode.MANTLE) {
            dashStart=time-dt;dashDirection=wish.length()>0?wish:V3.look(input.yaw(),0);
            boolean ground=grounded;
            dashDuration=data.stat(ground?"EGroundDashDuration":"EAirDashDuration");
            dashDistance=data.stat(ground?"EGroundDashDistanceInMeters":"EAirDashDistanceInMeters");
            mode=ground?Mode.DASH_GROUND:Mode.DASH_AIR;stamina-=1;
            if(!ground) airDashUsed=true;
        }
        if(mode==Mode.MANTLE) {
            double t=Math.min(1,dt/Math.max(dt,mantleLeft));
            V3 move=mantleVertical?new V3(0,mantleRemaining.y()*t,0):mantleRemaining.flat().mul(t);
            MoveResult result=collide(world,move,false,dt);mantleRemaining=mantleRemaining.sub(result.actual());mantleLeft-=dt;
            if(mantleLeft<=1e-9) {
                if(mantleVertical&&Math.abs(mantleRemaining.y())<1e-4){mantleVertical=false;mantleLeft=mantleHorizontalTime;}
                else {mode=grounded?Mode.RUN:Mode.AIR;velocity=wish.mul(data.stat("EMaxMoveSpeed"));}
            }
            return;
        }
        if(input.edge(Controls.JUMP)) jump(input,world);
        if(mode==Mode.MANTLE)return;
        if(input.down(Controls.JUMP) && mode!=Mode.ROPE && world.rope()) mode=Mode.ROPE;
        if(mode==Mode.ROPE) {
            if(!world.rope() || input.edge(Controls.JUMP)) {
                mode=Mode.AIR;
                velocity=V3.look(input.yaw(),0).mul(f("citadel_ability_climb_rope","m_flJumpOffHorizontal")*Retail.SOURCE_UNIT)
                    .withY(f("citadel_ability_climb_rope","m_flJumpOffVertical")*Retail.SOURCE_UNIT);
            } else {
                double up=input.down(Controls.CROUCH)?-f("citadel_ability_climb_rope","m_flClimbSpeedDown"):
                    (input.down(Controls.FORWARD)||input.down(Controls.JUMP)?f("citadel_ability_climb_rope","m_flClimbSpeedUp"):0);
                velocity=new V3(0,up*Retail.SOURCE_UNIT,0);
                collide(world,velocity.mul(dt),false,dt); return;
            }
        }
        V3 zip=world.zipDirection();
        if(mode==Mode.ZIPLINE) {
            if(zip==null || input.edge(Controls.JUMP) || input.down(Controls.CROUCH)) {
                mode=Mode.AIR;velocity=velocity.withY(input.down(Controls.CROUCH)?0:p("citadel_ability_zip_line","DismountVerticalSpeed")*Retail.SOURCE_UNIT);
            } else {
                zipSpeed=Math.min(p("citadel_ability_zip_line","ZipSpeedOuter")*Retail.SOURCE_UNIT,zipSpeed+p("citadel_ability_zip_line","ZipAcc")*Retail.SOURCE_UNIT*dt);
                velocity=zip.mul(zipSpeed);collide(world,velocity.mul(dt),false,dt);return;
            }
        } else if(zip!=null && input.down(Controls.JUMP)) { mode=Mode.ZIPLINE;zipSpeed=p("citadel_ability_zip_line","LatchInitialSpeed")*Retail.SOURCE_UNIT; }
        boolean dash=mode==Mode.DASH_GROUND || mode==Mode.DASH_AIR;
        if(dash) {
            double elapsed=time-dashStart;
            if(input.down(Controls.CROUCH) && grounded && elapsed>=f("citadel_ability_slide","m_flDashSlideStartTime")) {
                mode=Mode.SLIDE;slideStart=time;
                velocity=dashDirection.mul(f("citadel_ability_slide","m_flDashSlideSpeed")*Retail.SOURCE_UNIT);dash=false;
            } else if(elapsed-dt<dashDuration) {
                double t0=Math.max(0,(elapsed-dt)/dashDuration),t1=Math.min(1,elapsed/dashDuration);
                double fraction;
                if(mode==Mode.DASH_GROUND) {
                    JsonObject curve=data.hero.getAsJsonObject("m_groundDashPositionCurve");
                    fraction=Curve.sample(curve,t1)-Curve.sample(curve,t0);
                } else fraction=t1-t0;
                V3 displacement=dashDirection.mul(dashDistance*fraction);
                velocity=displacement.mul(1/dt).withY(mode==Mode.DASH_AIR?0:velocity.y());
                if(mode==Mode.DASH_GROUND)gravity(dt);
                collide(world,displacement.add(new V3(0,velocity.y()*dt,0)),false,dt);return;
            } else {
                if(mode==Mode.DASH_AIR) velocity=velocity.mul(f("citadel_ability_dash","m_flAirDashEndVelocityScale"));
                mode=grounded?Mode.RUN:Mode.AIR;dash=false;
            }
        }
        double max=data.stat("EMaxMoveSpeed")+bonusSpeed+sprintBonus;
        if(input.down(Controls.CROUCH) && grounded && mode!=Mode.SLIDE) {
            if(velocity.horizontal()>=f("citadel_ability_slide","m_flMinActivationSpeed")*Retail.SOURCE_UNIT) { mode=Mode.SLIDE;slideStart=time; }
            else { mode=Mode.CROUCH;max=data.stat("ECrouchSpeed")+bonusSpeed; }
        }
        if(mode==Mode.SLIDE) {
            if(!input.down(Controls.CROUCH) || velocity.horizontal()<f("citadel_ability_slide","m_flMinSustainSpeed")*Retail.SOURCE_UNIT) mode=grounded?Mode.RUN:Mode.AIR;
            else {
                double speed=velocity.horizontal();
                double friction=time-slideStart<f("citadel_ability_slide","m_flFlatGroundFrictionGraceTime")?
                    f("citadel_ability_slide","m_flFrictionFlatGroundGrace"):f("citadel_ability_slide","m_flFrictionFlatGround");
                if(slope<0) {
                    double acc=f("citadel_ability_slide","m_flSlideMaxSlopeAcceleration")*Retail.SOURCE_UNIT*Math.min(1,Math.abs(slope)/45);
                    speed=Math.min(f("citadel_ability_slide","m_flSlideMaxSlopeMaxAccSpeed")*Retail.SOURCE_UNIT,speed+acc*dt);
                    friction=f("citadel_ability_slide","m_flFrictionMinSlope");
                }
                V3 dir=velocity.flat().normalized();
                if(wish.length()>0) dir=dir.lerp(wish,Math.min(1,f("citadel_ability_slide","m_flTurnSpeed")*dt/180)).normalized();
                velocity=dir.mul(Math.max(0,speed-speed*friction*dt)).withY(velocity.y());
                gravity(dt);collide(world,velocity.mul(dt),true,dt);return;
            }
        }
        if(capSpeed>=0) max=Math.min(max,capSpeed);
        if(input.down(Controls.FIRE)) max*=data.weapon().get("m_flShootMoveSpeedPercent").getAsDouble();
        if(input.down(Controls.AIM)) max*=data.weapon().get("m_flZoomMoveSpeedPercent").getAsDouble();
        if(time<parryUntil) max*=p("citadel_ability_melee_parry","ParryMoveSpeed")/100;
        double speed=velocity.horizontal();
        if(grounded && speed>0) {
            double drop=Math.max(speed,data.nativeFloat("sv_stopspeed")*Retail.SOURCE_UNIT)*data.nativeFloat("sv_friction")*dt;
            velocity=velocity.flat().mul(Math.max(0,speed-drop)/speed).withY(velocity.y());
        }
        double current=velocity.dot(wish),add=max-current;
        if(add>0) {
            double accel=grounded?data.stat("EMoveAcceleration"):data.nativeFloat("sv_airaccelerate");
            velocity=velocity.add(wish.mul(Math.min(add,accel*max*dt)));
        }
        if(!grounded) {
            double drag=data.nativeFloat("citadel_air_drag_min");
            velocity=velocity.mul(Math.max(0,1-drag*dt)).withY(velocity.y());
        }
        gravity(dt);
        collide(world,velocity.mul(dt),input.down(Controls.CROUCH),dt);
        if(!grounded) mode=Mode.AIR;
        else if(input.down(Controls.CROUCH)) mode=Mode.CROUCH;
        else mode=wish.length()>0?(sprintBonus>0?Mode.SPRINT:Mode.RUN):Mode.IDLE;
        if(!velocity.finite()) throw new IllegalStateException("Non-finite Haze velocity");
    }
    private void jump(Controls in,Collision world) {
        if(mode==Mode.ROPE || mode==Mode.ZIPLINE) return;
        if((mode==Mode.DASH_GROUND || mode==Mode.DASH_AIR) && time-dashStart>=f("citadel_ability_jump","m_flDashJumpStartTime") && time-dashStart<=f("citadel_ability_jump","m_flDashJumpEndTime") && stamina>=1) {
            stamina-=1;velocity=dashDirection.mul(f("citadel_ability_jump","m_flDashJumpDistanceInMeters")/dashDuration)
                .withY(f("citadel_ability_jump","m_flDashJumpVerticalSpeed")*Retail.SOURCE_UNIT);mode=Mode.AIR;grounded=false;jumpStart=time;return;
        }
        if(wall) {
            V3 target=world.mantle(in.wish(),160*Retail.SOURCE_UNIT);
            if(target!=null) {
                mantleRemaining=target;double height=target.y()/Retail.SOURCE_UNIT;
                JsonArray types=data.ability("citadel_ability_mantle").getAsJsonArray("m_vecMantleTypes");
                JsonObject type=types.get(types.size()-1).getAsJsonObject();
                for(JsonElement t:types) if(height<=Retail.number(t.getAsJsonObject().get("m_flMaxHeight"))) { type=t.getAsJsonObject();break; }
                mantleHeight=(int)Retail.number(type.get("m_flAnimHeight"));
                mantleLeft=Retail.number(type.get("m_flVerticalTime"));mantleHorizontalTime=Retail.number(type.get("m_flHorizontalTime"));mantleVertical=true;
                mode=Mode.MANTLE;mantleStart=time;velocity=V3.ZERO;return;
            }
            double power=Math.min(1,(time-lastWallJump)/f("citadel_ability_jump","m_flWallJumpFullPowerRechargeTime"));
            power=Math.max(f("citadel_ability_jump","m_flWallJumpPowerMin"),power);
            velocity=wallNormal.mul(f("citadel_ability_jump","m_flWallJumpMaxOutSpeed")*Retail.SOURCE_UNIT*power)
                .withY(f("citadel_ability_jump","m_flWallJumpUpSpeed")*Retail.SOURCE_UNIT*power);
            lastWallJump=time;mode=Mode.AIR;grounded=false;jumpStart=time;return;
        }
        if(grounded || time-lastGroundTime<=f("citadel_ability_jump","m_flLateJumpGraceWindow")) {
            velocity=velocity.withY(p("citadel_ability_jump","VerticalSpeed")*Retail.SOURCE_UNIT);grounded=false;mode=Mode.AIR;lastGroundTime=-100;jumpStart=time;
        } else if(!airJumpUsed && stamina>=1) {
            stamina-=1;airJumpUsed=true;
            velocity=velocity.withY(p("citadel_ability_jump","VerticalSpeed")*p("citadel_ability_jump","AirJumpVerticalSpeedPercent")/100*Retail.SOURCE_UNIT);mode=Mode.AIR;jumpStart=time;
        }
    }
    private void gravity(double dt) {
        if(grounded && velocity.y()<=0) velocity=velocity.withY(-data.nativeFloat("sv_gravity")*Retail.SOURCE_UNIT*dt);
        else velocity=velocity.withY(velocity.y()-data.nativeFloat("sv_gravity")*Retail.SOURCE_UNIT*dt);
        double terminal=data.nativeFloat("sv_maxvelocity")*Retail.SOURCE_UNIT;
        velocity=velocity.withY(Math.max(-terminal,velocity.y()));
    }
    private MoveResult collide(Collision world,V3 displacement,boolean crouch,double dt) {
        MoveResult r=world.move(displacement,crouch);
        grounded=r.ground();wall=r.wall();wallNormal=r.wallNormal();slope=r.slope();
        if(Math.abs(r.actual().x()-displacement.x())>1e-7) velocity=new V3(0,velocity.y(),velocity.z());
        if(Math.abs(r.actual().z()-displacement.z())>1e-7) velocity=new V3(velocity.x(),velocity.y(),0);
        if(Math.abs(r.actual().y()-displacement.y())>1e-7) velocity=velocity.withY(0);
        return r;
    }
}
