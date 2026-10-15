package dev.local.haze.sim;

import com.google.gson.*;
import java.util.*;
import dev.local.haze.sim.movement.*;
import static dev.local.haze.sim.movement.CharacterMovementProfile.Role.*;

/** Guest-space motion. Collision callbacks are the only Minecraft dependency. */
public final class Motion {
    public enum Mode { IDLE, RUN, SPRINT, CROUCH, DASH_GROUND, DASH_AIR, SLIDE, AIR, MANTLE, MELEE }
    public record MoveResult(V3 actual,boolean ground,boolean wall,V3 wallNormal,double slope) {}
    public interface Collision {
        MoveResult move(V3 displacement,boolean crouch);
        default V3 mantle(V3 direction,double maxHeight) { return null; }
        default V3 wallContact() { return V3.ZERO; }
    }
    private final CharacterMovementProfile profile;
    private final Map<String,Double> nativeValues;
    private final double shootScale,zoomScale;
    private final double staminaRegenMultiplier;
    private final JsonObject dashCurve;
    private static final double UNIT=0.0254;
    public V3 velocity=V3.ZERO;
    public Mode mode=Mode.IDLE;
    public double time,stamina,dashStart=-100,dashDuration,dashDistance,lastGroundTime=-100,
        lastWallJump=-100,slideStart=-100,jumpStart=-100,mantleStart=-100,parryUntil,parryReady,meleeStart=-100,sprintBonus;
    public V3 dashDirection=V3.ZERO,wallNormal=V3.ZERO;
    public boolean grounded=true,wall,airJumpUsed,airDashUsed,downDashUsed;
    public int mantleHeight=32;
    public int wallJumps;
    private MantleTrajectory mantleTrajectory;
    private V3 mantlePosition=V3.ZERO;
    private double mantleEnd,dashJumpUntil=-100,parryCancelTime,parrySlideUntil;
    private double lastPaidJump=-100,lastPaidCost;
    private final List<Double> regenPenalties=new ArrayList<>();
    private double slope;
    private boolean combatThisStep;
    public Motion(Retail data) { this(data,1); }
    public Motion(Retail data,double staminaRegenMultiplier) { this(data.movementProfile(),data.movementDefaults(),
        Retail.number(data.weapon().get("m_flShootMoveSpeedPercent")),Retail.number(data.weapon().get("m_flZoomMoveSpeedPercent")),staminaRegenMultiplier); }
    public Motion(CharacterMovementProfile profile,Map<String,Double> nativeValues,double shootScale,double zoomScale) {
        this(profile,nativeValues,shootScale,zoomScale,1);
    }
    public Motion(CharacterMovementProfile profile,Map<String,Double> nativeValues,double shootScale,double zoomScale,double staminaRegenMultiplier) {
        if(!Double.isFinite(staminaRegenMultiplier)||staminaRegenMultiplier<0)throw new IllegalArgumentException("Stamina regeneration multiplier");
        this.profile=profile;this.nativeValues=Map.copyOf(nativeValues);this.shootScale=shootScale;this.zoomScale=zoomScale;
        this.staminaRegenMultiplier=staminaRegenMultiplier;
        dashCurve=profile.groundDashCurve().getAsJsonObject();stamina=profile.stat("EStamina");
    }
    private double f(CharacterMovementProfile.Role role,String key) { return profile.field(role,key); }
    private double p(CharacterMovementProfile.Role role,String key) { return profile.baseProperty(role,key); }
    private double n(String key) { Double value=nativeValues.get(key);if(value==null)throw new IllegalArgumentException("Missing movement rule: "+key);return value; }
    public double maxStamina() { return profile.stat("EStamina"); }
    public void step(double dt,Controls input,Collision world,double bonusSpeed,double capSpeed) {
        step(dt,input,world,bonusSpeed,capSpeed,false);
    }
    public void step(double dt,Controls input,Collision world,double bonusSpeed,double capSpeed,boolean inCombat) {
        if(dt<=0 || dt>0.05) throw new IllegalArgumentException("Motion substep");
        time+=dt;combatThisStep=inCombat;
        regenPenalties.removeIf(end->time>=end);
        PercentageStack regen=new PercentageStack();
        for(double ignored:regenPenalties)regen.add((float)p(JUMP,"StaminaRegenReduction"));
        stamina=Math.min(maxStamina(),stamina+profile.stat("EStaminaRegenPerSecond")*staminaRegenMultiplier*regen.multiplier()*dt);
        if(grounded) { lastGroundTime=time;airJumpUsed=false;airDashUsed=false;downDashUsed=false;wallJumps=0; }
        V3 contact=world.wallContact();
        if(contact.length()>0) { wall=true;wallNormal=contact.normalized(); }
        if(input.edge(Controls.PARRY) && time>=parryReady) {
            parryUntil=time+f(PARRY,"m_flActiveTime");
            parryReady=time+p(PARRY,"AbilityCooldown");
            if(mode==Mode.DASH_GROUND || mode==Mode.DASH_AIR) {
                double scale=f(DASH,"m_flParryCancelSpeedScale");
                if(mode==Mode.DASH_GROUND) {
                    velocity=dashDirection.mul(profile.stat("EGroundDashDistanceInMeters")/profile.stat("EGroundDashDuration")*scale).withY(velocity.y());
                    parrySlideUntil=time+f(DASH,"m_flParryCancelSlideDuration");
                } else {
                    double cap=profile.stat("EAirDashDistanceInMeters")/profile.stat("EAirDashDuration")*scale;
                    if(velocity.horizontal()>cap)velocity=velocity.flat().normalized().mul(cap).withY(velocity.y());
                    parryCancelTime=time;
                }
                mode=grounded?Mode.RUN:Mode.AIR;
            }
        }
        V3 wish=input.wish();
        boolean sprint=!inCombat&&wish.length()>0&&!input.down(Controls.CROUCH)&&!input.down(Controls.AIM)&&!input.down(Controls.FIRE)&&!input.down(Controls.MELEE)&&capSpeed<0;
        sprintBonus=sprint?Math.min(profile.stat("ESprintSpeed"),sprintBonus+f(SPRINT,"m_flSprintAccMS")*dt):0;
        if(input.edge(Controls.DOWN_DASH) && !grounded && !downDashUsed && stamina>=.5 && mode!=Mode.MANTLE) {
            stamina-=.5;downDashUsed=true;mode=Mode.AIR;
            velocity=velocity.withY(-f(DASH,"m_flDownwardAirDashSpeed")*UNIT);
        }
        if(input.edge(Controls.DASH) && wish.length()>0 && stamina>=1 && (grounded || !airDashUsed) && mode!=Mode.MANTLE) {
            dashStart=time-dt;dashDirection=wish;
            boolean ground=grounded;
            dashDuration=profile.stat(ground?"EGroundDashDuration":"EAirDashDuration");
            dashDistance=profile.stat(ground?"EGroundDashDistanceInMeters":"EAirDashDistanceInMeters");
            mode=ground?Mode.DASH_GROUND:Mode.DASH_AIR;stamina-=1;
            if(!ground) airDashUsed=true;
        }
        if(mode==Mode.MANTLE) {
            V3 target=mantleTrajectory.sample((float)time);
            MoveResult result=collide(world,target.sub(mantlePosition),false,dt);
            mantlePosition=mantlePosition.add(result.actual());
            if(time>=mantleEnd) {
                mode=grounded?Mode.RUN:Mode.AIR;velocity=wish.mul(profile.stat("EMaxMoveSpeed"));
            }
            return;
        }
        // Native action slots: held mantle (10) consumes Space before jump (12).
        if((input.down(Controls.JUMP)||input.edge(Controls.JUMP)) && wall && time>=dashJumpUntil && tryMantle(input,world))return;
        if(input.edge(Controls.JUMP)) jump(input,world);
        if(mode==Mode.MANTLE)return;
        boolean dash=mode==Mode.DASH_GROUND || mode==Mode.DASH_AIR;
        if(dash) {
            double elapsed=time-dashStart;
            if(input.down(Controls.CROUCH) && grounded && elapsed>=f(SLIDE,"m_flDashSlideStartTime")) {
                mode=Mode.SLIDE;slideStart=time;
                velocity=dashDirection.mul(Math.max(velocity.horizontal(),f(SLIDE,"m_flDashSlideSpeed")*UNIT)).withY(velocity.y());dash=false;
            } else if(elapsed-dt<dashDuration) {
                double t0=Math.max(0,(elapsed-dt)/dashDuration),t1=Math.min(1,elapsed/dashDuration);
                double fraction;
                if(mode==Mode.DASH_GROUND) {
                    JsonObject curve=dashCurve;
                    fraction=Curve.sample(curve,t1)-Curve.sample(curve,t0);
                } else fraction=t1-t0;
                V3 displacement=dashDirection.mul(dashDistance*fraction);
                // The final dash fragment covers less time than this simulation step.
                // Its displacement must not become an artificially tiny exit velocity.
                double activeTime=(t1-t0)*dashDuration;
                V3 dashVelocity=activeTime>1e-9?displacement.mul(1/activeTime):velocity.flat();
                velocity=dashVelocity.withY(mode==Mode.DASH_AIR?0:velocity.y());
                if(mode==Mode.DASH_GROUND)gravity(dt);
                collide(world,displacement.add(new V3(0,velocity.y()*dt,0)),false,dt);return;
            } else {
                // Keep momentum on release. The source end-scale belongs to the
                // unfinished dash speed callback, not a second exit impulse.
                mode=grounded?Mode.RUN:Mode.AIR;dash=false;
            }
        }
        double max=profile.stat("EMaxMoveSpeed")+bonusSpeed+sprintBonus;
        if(input.down(Controls.CROUCH) && grounded && mode!=Mode.SLIDE) {
            if(velocity.horizontal()>=f(SLIDE,"m_flMinActivationSpeed")*UNIT) { mode=Mode.SLIDE;slideStart=time; }
            else { mode=Mode.CROUCH;max=profile.stat("ECrouchSpeed")+bonusSpeed; }
        }
        if(mode==Mode.SLIDE) {
            if(!input.down(Controls.CROUCH) || velocity.horizontal()<f(SLIDE,"m_flMinSustainSpeed")*UNIT) mode=grounded?Mode.RUN:Mode.AIR;
            else {
                double speed=velocity.horizontal();
                double friction=time-slideStart<f(SLIDE,"m_flFlatGroundFrictionGraceTime")?
                    f(SLIDE,"m_flFrictionFlatGroundGrace"):f(SLIDE,"m_flFrictionFlatGround");
                if(slope<0) {
                    double acc=f(SLIDE,"m_flSlideMaxSlopeAcceleration")*UNIT*Math.min(1,Math.abs(slope)/45);
                    speed=Math.min(f(SLIDE,"m_flSlideMaxSlopeMaxAccSpeed")*UNIT,speed+acc*dt);
                    friction=f(SLIDE,"m_flFrictionMinSlope");
                }
                V3 dir=velocity.flat().normalized();
                if(wish.length()>0) {
                    double angle=Math.atan2(dir.x()*wish.z()-dir.z()*wish.x(),dir.dot(wish));
                    double turn=Math.toRadians(f(SLIDE,"m_flTurnSpeed"))*dt*Math.max(0,1-Math.abs(Math.toDegrees(angle))/45);
                    double rotate=Math.clamp(angle,-turn,turn),cos=Math.cos(rotate),sin=Math.sin(rotate);
                    dir=new V3(dir.x()*cos-dir.z()*sin,0,dir.x()*sin+dir.z()*cos);
                }
                velocity=dir.mul(Math.max(0,speed-speed*friction*dt)).withY(velocity.y());
                gravity(dt);collide(world,velocity.mul(dt),true,dt);return;
            }
        }
        if(capSpeed>=0) max=Math.min(max,capSpeed);
        if(input.down(Controls.FIRE)) max*=shootScale;
        if(input.down(Controls.AIM)) max*=zoomScale;
        if(time<parryUntil) max*=p(PARRY,"ParryMoveSpeed")/100;
        double speed=velocity.horizontal();
        if(grounded && speed>0) {
            double friction=n("sv_friction");
            if(time<parrySlideUntil)friction*=1+f(DASH,"m_flParryCancelSlideFrictionPercent")/100;
            double drop=Math.max(speed,n("sv_stopspeed")*UNIT)*friction*dt;
            velocity=velocity.flat().mul(Math.max(0,speed-drop)/speed).withY(velocity.y());
        }
        double wishSpeed=max;
        if(!grounded) {
            double blend=Math.clamp((velocity.length()/UNIT-600)/200,0,1);
            double control=(n("citadel_aircontrol_speed_slow")*(1-blend)+n("citadel_aircontrol_speed_fast")*blend)*UNIT;
            if(time<dashJumpUntil+f(JUMP,"m_flDashJumpExtraAirControlTime")-f(JUMP,"m_flDashJumpMantleDisableTime"))
                control*=1+f(JUMP,"m_flDashJumpExtraAirControlPercent")/100;
            wishSpeed=Math.min(max,control);
        }
        double current=velocity.dot(wish),add=wishSpeed-current;
        if(add>0) {
            double accel=grounded?profile.stat("EMoveAcceleration"):n("sv_airaccelerate");
            double accelerationSpeed=grounded?Math.max(max,n("citadel_min_accel_speed")*UNIT):wishSpeed;
            velocity=velocity.add(wish.mul(Math.min(add,accel*accelerationSpeed*dt)));
        }
        if(!grounded) {
            double drag=n("citadel_air_drag_min");
            velocity=velocity.mul(Math.max(0,1-drag*dt)).withY(velocity.y());
        }
        gravity(dt);
        collide(world,velocity.mul(dt),input.down(Controls.CROUCH),dt);
        if(!grounded) mode=Mode.AIR;
        else if(input.down(Controls.CROUCH)) mode=Mode.CROUCH;
        else mode=wish.length()>0?(sprintBonus>0?Mode.SPRINT:Mode.RUN):Mode.IDLE;
        if(!velocity.finite()) throw new IllegalStateException("Non-finite Haze velocity");
    }
    private boolean tryMantle(Controls in,Collision world) {
        V3 direction=in.wish().length()>0?in.wish():V3.look(in.yaw(),0);
        V3 target=world.mantle(direction,134*UNIT);
        if(target==null)return false;
        double height=target.y()/UNIT,best=Double.POSITIVE_INFINITY;
        JsonObject selected=null;
        for(JsonElement item:profile.definition(MANTLE).getAsJsonArray("m_vecMantleTypes")) {
            JsonObject type=item.getAsJsonObject();
            double min=Retail.number(type.get("m_flMinHeight")),max=Retail.number(type.get("m_flMaxHeight"));
            double distance=Math.abs(height-Retail.number(type.get("m_flAnimHeight")));
            if(height>=min && height<=max && distance<best){selected=type;best=distance;}
        }
        if(selected==null)return false;
        mantleHeight=(int)Retail.number(selected.get("m_flAnimHeight"));
        // Host probe supplies the endpoint; native helper supplies timing and height correction.
        mantleTrajectory=new MantleTrajectory(V3.ZERO,target.withY(mantleHeight*UNIT),(float)time,
            (float)Retail.number(selected.get("m_flVerticalTime")),(float)Retail.number(selected.get("m_flHorizontalTime")),
            (float)MovementClock.INTERVAL,0,(float)(target.y()-mantleHeight*UNIT));
        mantleEnd=time+mantleTrajectory.roundedVerticalTime()+mantleTrajectory.roundedHorizontalTime();
        mantlePosition=V3.ZERO;mode=Mode.MANTLE;mantleStart=time;velocity=V3.ZERO;
        if(time-lastPaidJump<=f(JUMP,"m_flMantleRefundWindow")) {
            stamina=Math.min(maxStamina(),stamina+lastPaidCost);lastPaidJump=-100;lastPaidCost=0;
        }
        return true;
    }
    private void payJump(double cost) {
        stamina-=cost;
        if(cost>0){lastPaidJump=time;lastPaidCost=cost;}
    }
    private void jump(Controls in,Collision world) {
        if(mode==Mode.DASH_GROUND || mode==Mode.DASH_AIR) {
            double elapsed=time-dashStart;
            if(elapsed>=f(JUMP,"m_flDashJumpStartTime") && elapsed<=f(JUMP,"m_flDashJumpEndTime") && stamina>=1) {
                payJump(1);
                double up=f(JUMP,"m_flDashJumpVerticalSpeed")*UNIT;
                double flightTime=2*up/(n("sv_gravity")*UNIT);
                velocity=dashDirection.mul(f(JUMP,"m_flDashJumpDistanceInMeters")/flightTime).withY(up);
                mode=Mode.AIR;grounded=false;jumpStart=time;lastGroundTime=-100;
                dashJumpUntil=time+f(JUMP,"m_flDashJumpMantleDisableTime");
            }
            return;
        }
        if(wall && !grounded) {
            double cost=wallJumps<n("citadel_wall_jump_num_free_wall_jumps")?0:
                n(wallJumps==0?"citadel_initial_wall_jump_stamina_cost":"citadel_wall_jump_stamina_cost");
            if(stamina<=0 || stamina<cost)return;
            payJump(cost);wallJumps++;
            double power=SourceMath.bias(SourceMath.remapClamped((float)(time-lastWallJump),0,
                (float)f(JUMP,"m_flWallJumpFullPowerRechargeTime"),(float)f(JUMP,"m_flWallJumpPowerMin"),1),
                (float)f(JUMP,"m_flWallJumpPowerBias"));
            V3 normal=wallNormal.flat().normalized(),wish=in.wish(),old=velocity;
            double outward=(f(JUMP,"m_flWallJumpMinOutSpeed")+(f(JUMP,"m_flWallJumpMaxOutSpeed")-
                f(JUMP,"m_flWallJumpMinOutSpeed"))*Math.clamp((wish.dot(normal)+1)/2,0,1))*UNIT;
            double inward=old.flat().dot(normal);
            V3 flat=old.flat().sub(normal.mul(Math.min(0,inward)));
            flat=flat.add(normal.mul(Math.max(0,outward-flat.dot(normal))));
            V3 tangent=new V3(-normal.z(),0,normal.x());
            double along=old.dot(tangent),lateral=wish.dot(tangent)*f(JUMP,"m_flWallJumpMaxLateralSpeed")*UNIT;
            double curveInput=Math.abs(along)/UNIT*(Math.signum(along)==Math.signum(lateral)?1:.5);
            lateral*=Curve.sample(profile.definition(JUMP).getAsJsonObject("m_WallJumpLateralSpeedFalloffVsAlongSpeed"),curveInput);
            double up=f(JUMP,"m_flWallJumpUpSpeed")*UNIT*power+(old.y()<-2*UNIT?old.y()*.3:0);
            velocity=flat.add(tangent.mul(lateral)).withY(up);
            if(combatThisStep)regenPenalties.add(time+p(JUMP,"StaminaRegenReductionDuration"));
            lastWallJump=time;mode=Mode.AIR;grounded=false;jumpStart=time;lastGroundTime=-100;return;
        }
        if(grounded || (velocity.y()<0 && time-lastGroundTime<f(JUMP,"m_flLateJumpGraceWindow"))) {
            velocity=velocity.withY(p(JUMP,"VerticalSpeed")*UNIT);grounded=false;mode=Mode.AIR;lastGroundTime=-100;jumpStart=time;
        } else if(!airJumpUsed && stamina>=1 && n("citadel_air_jumps_enabled")!=0) {
            payJump(1);airJumpUsed=true;
            V3 wish=in.wish();
            double missing=Math.max(0,profile.stat("EMaxMoveSpeed")-velocity.dot(wish));
            velocity=velocity.add(wish.mul(Math.min(missing,f(JUMP,"m_flMaxSpeedDelta")*UNIT)))
                .withY(p(JUMP,"VerticalSpeed")*p(JUMP,"AirJumpVerticalSpeedPercent")/100*UNIT);
            mode=Mode.AIR;jumpStart=time;
        }
    }
    private void gravity(double dt) {
        DashParryGravity glide=DashParryGravity.at((float)parryCancelTime,(float)time,
            (float)f(DASH,"m_flParryCancelAirGlideDuration"),(float)f(DASH,"m_flParryCancelAirGravityScale"));
        double gravity=n("sv_gravity")*UNIT*(glide.active()?glide.scale():1);
        if(grounded && velocity.y()<=0) velocity=velocity.withY(-gravity*dt);
        else velocity=velocity.withY(velocity.y()-gravity*dt);
        double terminal=n("sv_maxvelocity")*UNIT;
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
