package dev.local.haze.sim;

import com.google.gson.*;
import java.util.*;
import dev.local.haze.sim.movement.*;
import static dev.local.haze.sim.movement.CharacterMovementProfile.Role.*;

/** Guest-space motion. Collision callbacks are the only Minecraft dependency. */
public final class Motion {
    public enum Mode { IDLE, RUN, SPRINT, CROUCH, DASH_GROUND, DASH_AIR, SLIDE, AIR, MANTLE, MELEE }
    public record MoveResult(V3 actual,boolean ground,boolean wall,V3 wallNormal,double slope,V3 resolvedVelocity) {
        public MoveResult(V3 actual,boolean ground,boolean wall,V3 wallNormal,double slope){this(actual,ground,wall,wallNormal,slope,null);}
    }
    public interface Collision {
        MoveResult move(V3 displacement,boolean crouch);
        default MoveResult move(V3 displacement,boolean crouch,V3 velocity,double dt){return move(displacement,crouch);}
        default V3 mantle(V3 direction,double maxHeight) { return null; }
        default V3 wallContact() { return V3.ZERO; }
        default boolean canStand(){return true;}
    }
    private final CharacterMovementProfile profile;
    private final Map<String,Double> nativeValues;
    private final double shootScale,zoomScale;
    private double staminaRegenMultiplier;
    private final JsonObject dashCurve;
    private static final double UNIT=0.0254;
    public V3 velocity=V3.ZERO;
    public Mode mode=Mode.IDLE;
    public double time,stamina,dashStart=-100,dashDuration,dashDistance,lastGroundTime=-100,
        lastWallJump=-100,slideStart=-100,jumpStart=-100,mantleStart=-100,parryUntil,parryReady,meleeStart=-100,sprintBonus;
    public V3 dashDirection=V3.ZERO,wallNormal=V3.ZERO;
    public enum JumpKind { GROUND, AIR, WALL, DASH }
    public long jumpSerial;
    public JumpKind jumpKind=JumpKind.GROUND;
    public String jumpFacing="forward",dashFacing="forward";
    public boolean grounded=true,wall,airJumpUsed,airDashUsed,downDashUsed;
    public boolean ducked;
    public int mantleHeight=32;
    public int wallJumps;
    private MantleTrajectory mantleTrajectory;
    private V3 mantlePosition=V3.ZERO;
    private double mantleEnd,dashJumpUntil=-100,parryCancelTime,parrySlideUntil,airDashTravel,postDashTime=-100;
    private double slideExit=-100,getupStart=-100,groundedSince,landedTime=-100;
    private int slideJumps;
    private boolean dashSlide;
    private V3 airDashOffset=V3.ZERO;
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
    public void setStaminaRegenMultiplier(double value){
        if(!Double.isFinite(value)||value<0)throw new IllegalArgumentException("Stamina regeneration multiplier");
        staminaRegenMultiplier=value;
    }
    public boolean crouchedPose() { return grounded&&ducked; }
    public void step(double dt,Controls input,Collision world,double bonusSpeed,double capSpeed) {
        step(dt,input,world,bonusSpeed,capSpeed,false);
    }
    public void step(double dt,Controls input,Collision world,double bonusSpeed,double capSpeed,boolean inCombat) {
        if(dt<0 || dt>0.05 || (dt==0&&input.pressed()==0)) throw new IllegalArgumentException("Motion substep");
        time+=dt;combatThisStep=inCombat;
        ducked=(grounded&&input.down(Controls.CROUCH))||!world.canStand();
        regenPenalties.removeIf(end->time>=end);
        PercentageStack regen=new PercentageStack();
        for(double ignored:regenPenalties)regen.add((float)p(JUMP,"StaminaRegenReduction"));
        stamina=Math.min(maxStamina(),stamina+profile.stat("EStaminaRegenPerSecond")*staminaRegenMultiplier*regen.multiplier()*dt);
        if(grounded) { lastGroundTime=time;airJumpUsed=false;airDashUsed=false;downDashUsed=false;wallJumps=0; }
        if(grounded&&time-groundedSince>f(SLIDE,"m_flAirDragResetTime"))slideJumps=0;
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
            velocity=velocity.withY(-f(DASH,"m_flDownwardAirDashSpeed")*UNIT);postDashTime=-100;
        }
        if(input.edge(Controls.DASH) && wish.length()>0 && stamina>=1 && (grounded || !airDashUsed) && mode!=Mode.MANTLE) {
            dashStart=time-dt;dashDirection=wish;dashFacing=facing(wish,input.yaw());
            boolean ground=grounded;
            dashDuration=profile.stat(ground?"EGroundDashDuration":"EAirDashDuration");
            dashDistance=profile.stat(ground?"EGroundDashDistanceInMeters":"EAirDashDistanceInMeters");
            mode=ground?Mode.DASH_GROUND:Mode.DASH_AIR;stamina-=1;
            if(!ground) { airDashUsed=true;airDashTravel=0;airDashOffset=V3.ZERO; }
        }
        if(mode==Mode.MANTLE) {
            if(dt==0)return;
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
        if(!grounded&&ducked&&world.canStand())ducked=false;
        if(dt==0)return; // Input at a host-frame boundary changes action state without advancing physics.
        if(mode==Mode.MANTLE)return;
        boolean dash=mode==Mode.DASH_GROUND || mode==Mode.DASH_AIR;
        boolean airDashFinish=false;
        if(dash) {
            double elapsed=time-dashStart;
            if(input.down(Controls.CROUCH) && grounded && elapsed>=f(SLIDE,"m_flDashSlideStartTime")) {
                mode=Mode.SLIDE;slideStart=time;dashSlide=true;
                velocity=dashDirection.mul(f(SLIDE,"m_flDashSlideSpeed")*UNIT).withY(velocity.y());dash=false;
            } else if(mode==Mode.DASH_GROUND && elapsed-dt<dashDuration) {
                double t0=Math.max(0,(elapsed-dt)/dashDuration),t1=Math.min(1,elapsed/dashDuration);
                double fraction;
                fraction=Curve.sample(dashCurve,t1)-Curve.sample(dashCurve,t0);
                V3 displacement=dashDirection.mul(dashDistance*fraction);
                // The final dash fragment covers less time than this simulation step.
                // Its displacement must not become an artificially tiny exit velocity.
                double activeTime=(t1-t0)*dashDuration;
                V3 dashVelocity=activeTime>1e-9?displacement.mul(1/activeTime):velocity.flat();
                velocity=dashVelocity.withY(velocity.y());
                gravity(dt);
                collide(world,displacement.add(new V3(0,velocity.y()*dt,0)),false,dt);return;
            } else if(elapsed>=dashDuration || airDashTravel>dashDistance) {
                // Keep momentum on release. The source end-scale belongs to the
                // unfinished dash speed callback, not a second exit impulse.
                if(mode==Mode.DASH_AIR){postDashTime=time;airDashFinish=true;}
                mode=grounded?Mode.RUN:Mode.AIR;dash=false;
            }
        }
        boolean airDashing=mode==Mode.DASH_AIR;
        boolean airDashCommand=airDashing||airDashFinish;
        double airSpeedOverride=airDashing?dashDistance/dashDuration*(1+(f(DASH,"m_flAirDashEndVelocityScale")-1)*Math.clamp((time-dashStart)/dashDuration,0,1)):0;
        if(airDashCommand)wish=dashDirection;
        double max=profile.stat("EMaxMoveSpeed")+bonusSpeed+sprintBonus;
        if(input.down(Controls.CROUCH) && grounded && mode!=Mode.SLIDE) {
            if(velocity.horizontal()>=f(SLIDE,"m_flMinActivationSpeed")*UNIT) { mode=Mode.SLIDE;slideStart=time;dashSlide=false; }
            else { mode=Mode.CROUCH;max=profile.stat("ECrouchSpeed")+bonusSpeed; }
        }
        if(mode==Mode.SLIDE) {
            if(!grounded||!input.down(Controls.CROUCH) || velocity.horizontal()<f(SLIDE,"m_flMinSustainSpeed")*UNIT) {
                slideExit=time;if(grounded&&dashSlide)getupStart=time;mode=grounded?Mode.RUN:Mode.AIR;
            }
            else {
                double speed=velocity.horizontal();
                double friction=time-slideStart<f(SLIDE,"m_flFlatGroundFrictionGraceTime")||time-landedTime<f(SLIDE,"m_flLandedFlatGroundFrictionGraceTime")?
                    f(SLIDE,"m_flFrictionFlatGroundGrace"):f(SLIDE,"m_flFrictionFlatGround");
                if(slope<0) {
                    double acc=f(SLIDE,"m_flSlideMaxSlopeAcceleration")*UNIT*Math.min(1,Math.abs(slope)/45);
                    speed=Math.min(f(SLIDE,"m_flSlideMaxSlopeMaxAccSpeed")*UNIT,speed+acc*dt);
                    friction=f(SLIDE,"m_flFrictionMinSlope");
                }
                V3 dir=velocity.flat().normalized();
                if(wish.length()>0) {
                    double angle=Math.atan2(dir.x()*wish.z()-dir.z()*wish.x(),dir.dot(wish));
                    double rotate=Math.toRadians(Math.clamp(Math.toDegrees(angle),-1,1)*f(SLIDE,"m_flTurnSpeed")*dt*Math.max(0,1-Math.abs(Math.toDegrees(angle))/45)),cos=Math.cos(rotate),sin=Math.sin(rotate);
                    dir=new V3(dir.x()*cos-dir.z()*sin,0,dir.x()*sin+dir.z()*cos);
                }
                if(dashSlide&&time-slideStart<f(SLIDE,"m_flDashSlideFrictionTime"))friction=f(SLIDE,"m_flDashSlideFriction");
                velocity=dir.mul(Math.max(0,speed-Math.max(speed,n("sv_stopspeed")*UNIT)*friction*n("sv_friction")*dt)).withY(velocity.y());
                gravity(dt);collide(world,velocity.mul(dt),true,dt);return;
            }
        }
        if(grounded&&ducked)max=profile.stat("ECrouchSpeed")+bonusSpeed;
        if(time-getupStart<=profile.definition(SLIDE).getAsJsonObject("m_GetupSpeedCurve").getAsJsonArray("m_vDomainMaxs").get(0).getAsDouble())
            max*=JumpImpulse.bonusScale((float)Curve.sample(profile.definition(SLIDE).getAsJsonObject("m_GetupSpeedCurve"),time-getupStart));
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
            float forward=(input.down(Controls.FORWARD)?1:0)-(input.down(Controls.BACK)?1:0);
            float side=(input.down(Controls.RIGHT)?1:0)-(input.down(Controls.LEFT)?1:0);
            if(airDashCommand){forward=(float)dashDirection.dot(V3.look(input.yaw(),0));side=(float)dashDirection.dot(V3.look(input.yaw()+90,0));}
            double drag=n("citadel_air_drag_min"),postProgress=(time-postDashTime)/f(DASH,"m_flPostDragDuration");
            if(slideJumps>1)drag=Math.max(drag,SourceMath.remapClamped((float)(time-slideExit),0,(float)f(SLIDE,"m_flAirDragAfterJumpTime"),
                (float)f(SLIDE,"m_flAirDragAfterJump"),0));
            if(postProgress>0&&postProgress<1)drag=Math.max(drag,SourceMath.remapClamped((float)velocity.horizontal(),
                (float)f(DASH,"m_flAirSpeedForMinDrag"),(float)f(DASH,"m_flAirSpeedForMaxDrag"),0,(float)f(DASH,"m_flPostMaxDrag"))*postProgress);
            velocity=AirDrag.apply(velocity.mul(1/UNIT),input.yaw()+90,forward,side,(float)(airSpeedOverride/UNIT),
                (float)drag,(float)n("sv_stopspeed"),1,(float)dt).mul(UNIT);
            float controlScale=time<dashJumpUntil+f(JUMP,"m_flDashJumpExtraAirControlTime")-f(JUMP,"m_flDashJumpMantleDisableTime")?
                JumpImpulse.bonusScale((float)f(JUMP,"m_flDashJumpExtraAirControlPercent")):1;
            float accelerationScale=airDashing?JumpImpulse.bonusScale((float)f(DASH,"m_flAirDashAccPct")):1;
            velocity=AirAcceleration.apply(velocity.mul(1/UNIT),wish,(float)(Math.max(max,airSpeedOverride)/UNIT),(float)(airSpeedOverride/UNIT),
                (float)n("citadel_aircontrol_speed_slow"),(float)n("citadel_aircontrol_speed_fast"),controlScale,
                (float)n("sv_airaccelerate"),accelerationScale,(float)dt).mul(UNIT);
        }
        double current=velocity.dot(wish),add=wishSpeed-current;
        if(grounded&&add>0) {
            double accel=profile.stat("EMoveAcceleration");
            double accelerationSpeed=Math.max(max,n("citadel_min_accel_speed")*UNIT);
            velocity=velocity.add(wish.mul(Math.min(add,accel*accelerationSpeed*dt)));
        }
        if(airDashCommand)velocity=velocity.withY(Math.max(velocity.y(),n("sv_stopspeed")*UNIT)*dt*f(DASH,"m_flDuringDrag"));
        else gravity(dt);
        collide(world,velocity.mul(dt),input.down(Controls.CROUCH),dt);
        if(airDashing)return;
        if(!grounded) mode=Mode.AIR;
        else if(input.down(Controls.CROUCH)) mode=Mode.CROUCH;
        else mode=wish.length()>0?(sprintBonus>0?Mode.SPRINT:Mode.RUN):Mode.IDLE;
        if(!velocity.finite()) throw new IllegalStateException("Non-finite Haze velocity");
    }
    private boolean tryMantle(Controls in,Collision world) {
        V3 direction=in.wish().length()>0?in.wish():V3.look(in.yaw(),0);
        if(direction.dot(wallNormal.flat().normalized().mul(-1))<.7)return false;
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
        if(mode==Mode.DASH_GROUND) {
            double elapsed=time-dashStart;
            if(elapsed>=f(JUMP,"m_flDashJumpStartTime") && elapsed<=f(JUMP,"m_flDashJumpEndTime") && stamina>=1) {
                payJump(1);
                double up=f(JUMP,"m_flDashJumpVerticalSpeed")*UNIT;
                double flightTime=2*up/(n("sv_gravity")*UNIT);
                velocity=dashDirection.mul(f(JUMP,"m_flDashJumpDistanceInMeters")/flightTime).withY(up);
                mode=Mode.AIR;grounded=false;jumpStart=time;lastGroundTime=-100;
                dashJumpUntil=time+f(JUMP,"m_flDashJumpMantleDisableTime");
                jumped(JumpKind.DASH,in.yaw());
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
            velocity=WallJumpImpulse.apply(velocity.mul(1/UNIT),in.wish(),wallNormal,(float)n("citadel_corner_boost_strength"),
                (float)f(JUMP,"m_flWallJumpMinOutSpeed"),(float)f(JUMP,"m_flWallJumpMaxOutSpeed"),
                (float)f(JUMP,"m_flWallJumpUpSpeed"),(float)f(JUMP,"m_flWallJumpMaxLateralSpeed"),(float)power,
                value->Curve.sample(profile.definition(JUMP).getAsJsonObject("m_WallJumpLateralSpeedFalloffVsAlongSpeed"),value)).mul(UNIT);
            if(combatThisStep)regenPenalties.add(time+p(JUMP,"StaminaRegenReductionDuration"));
            jumped(JumpKind.WALL,in.yaw());lastWallJump=time;mode=Mode.AIR;grounded=false;jumpStart=time;lastGroundTime=-100;return;
        }
        if(grounded || (velocity.y()<0 && time-lastGroundTime<f(JUMP,"m_flLateJumpGraceWindow"))) {
            if(mode==Mode.SLIDE||time-slideExit<f(SLIDE,"m_flLateSlideJumpWindow"))slideJumps++;
            if(mode==Mode.SLIDE){slideExit=time;}
            if(time-getupStart<=profile.definition(SLIDE).getAsJsonObject("m_GetupSpeedCurve").getAsJsonArray("m_vDomainMaxs").get(0).getAsDouble())
                velocity=velocity.mul(f(JUMP,"m_flSlowedSlideJumpFactor"));
            velocity=velocity.withY(p(JUMP,"VerticalSpeed")*UNIT);grounded=false;mode=Mode.AIR;lastGroundTime=-100;jumpStart=time;
            jumped(JumpKind.GROUND,in.yaw());
        } else if(!airJumpUsed && stamina>=1 && n("citadel_air_jumps_enabled")!=0) {
            payJump(1);airJumpUsed=true;
            velocity=JumpImpulse.air(velocity.mul(1/UNIT),in.wish(),(float)(profile.stat("EMaxMoveSpeed")/UNIT),
                (float)f(JUMP,"m_flMaxSpeedDelta"),(float)p(JUMP,"VerticalSpeed"),
                (float)p(JUMP,"AirJumpVerticalSpeedPercent"),1).mul(UNIT);
            mode=Mode.AIR;jumpStart=time;
            jumped(JumpKind.AIR,in.yaw());
        }
    }
    private static String facing(V3 direction,float yaw){
        double forward=direction.dot(V3.look(yaw,0)),right=direction.dot(V3.look(yaw+90,0));
        return Math.abs(forward)>=Math.abs(right)?forward>=0?"forward":"back":right>=0?"right":"left";
    }
    private void jumped(JumpKind kind,float yaw){jumpKind=kind;jumpSerial++;jumpFacing=facing(velocity.flat(),yaw);}
    public void rotateView(float yawDelta){
        if(mode!=Mode.DASH_GROUND&&mode!=Mode.DASH_AIR)return;
        double angle=Math.toRadians(yawDelta),cos=Math.cos(angle),sin=Math.sin(angle);
        dashDirection=new V3(dashDirection.x()*cos-dashDirection.z()*sin,0,dashDirection.x()*sin+dashDirection.z()*cos);
    }
    private void gravity(double dt) {
        DashParryGravity glide=DashParryGravity.at((float)parryCancelTime,(float)time,
            (float)f(DASH,"m_flParryCancelAirGlideDuration"),(float)f(DASH,"m_flParryCancelAirGravityScale"));
        double gravity=n("sv_gravity")*UNIT*(glide.active()?glide.scale():1);
        if(grounded && velocity.y()<=0) velocity=velocity.withY(-gravity*dt);
        else velocity=velocity.withY(velocity.y()-gravity*dt);
    }
    private MoveResult collide(Collision world,V3 displacement,boolean crouch,double dt) {
        MoveResult r=world.move(displacement,(grounded&&crouch)||ducked,velocity,dt);
        if(r.ground()&&!grounded){landedTime=time;groundedSince=time;}
        grounded=r.ground();wall=r.wall();wallNormal=r.wallNormal();slope=r.slope();
        if(!grounded&&ducked&&world.canStand())ducked=false;
        if(mode==Mode.DASH_AIR){airDashOffset=airDashOffset.add(r.actual().flat());airDashTravel=airDashOffset.horizontal();}
        if(r.resolvedVelocity()!=null)velocity=r.resolvedVelocity();
        else {
            if(Math.abs(r.actual().x()-displacement.x())>1e-7) velocity=new V3(0,velocity.y(),velocity.z());
            if(Math.abs(r.actual().z()-displacement.z())>1e-7) velocity=new V3(velocity.x(),velocity.y(),0);
            if(Math.abs(r.actual().y()-displacement.y())>1e-7) velocity=velocity.withY(0);
        }
        return r;
    }
}
