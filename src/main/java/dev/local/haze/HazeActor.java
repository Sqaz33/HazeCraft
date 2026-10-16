package dev.local.haze;

import com.google.gson.*;
import dev.local.haze.sim.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import java.util.*;

public final class HazeActor {
    private static final Retail DATA=Retail.DATA;
    private static final ResourceKey<DamageType> BULLET=ResourceKey.create(Registries.DAMAGE_TYPE,Identifier.fromNamespaceAndPath("haze","bullet"));
    public final UUID owner;
    public boolean active;
    public net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension;
    public final Gun gun=new Gun(DATA);
    public final Abilities abilities=new Abilities(DATA);
    public final Map<Integer,Fixation> targets=new HashMap<>();
    public final List<Projectile> projectiles=new ArrayList<>();
    public final List<Trace> traces=new ArrayList<>();
    private long nextProjectile;
    public Controls input=Controls.NONE;
    public int lastSequence=-1,inputAge;
    private double oldMaxHealth,oldHealth,meleeHeld,meleeReady,parryReady,parryUntil,combatUntil;
    private boolean oldFlying;
    public String action="idle";
    public double actionTime;
    public double daggerStarted=-100;
    public double lastLeftShot=-100,lastRightShot=-100;
    public record Projectile(V3 position,V3 velocity,V3 origin,double remaining,boolean dagger,double damage,long id) {}
    public record Trace(long id,double time,V3 start,V3 end,boolean dagger){}
    public HazeActor(UUID owner) { this.owner=owner; }
    public void toggle(ServerPlayer p) {
        active=!active;
        if(active) {
            dimension=p.level().dimension();
            oldFlying=p.getAbilities().flying;p.getAbilities().flying=false;p.onUpdateAbilities();
            oldMaxHealth=p.getAttribute(Attributes.MAX_HEALTH).getBaseValue();oldHealth=p.getHealth();
            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(DATA.stat("EMaxHealth"));p.setHealth((float)DATA.stat("EMaxHealth"));
        } else {
            p.getAbilities().flying=oldFlying&&p.getAbilities().mayfly;p.onUpdateAbilities();
            p.getAttribute(Attributes.MAX_HEALTH).setBaseValue(oldMaxHealth);if(p.isAlive())p.setHealth((float)Math.min(oldHealth,p.getMaxHealth()));
            p.setInvisible(false);input=Controls.NONE;projectiles.clear();traces.clear();
            clearStatuses(p.level());targets.clear();
        }
        p.refreshDimensions();
    }
    public void accept(HazeProtocol.Input packet,ServerPlayer p) {
        if(packet.sequence()<=lastSequence || !Float.isFinite(packet.yaw()) || !Float.isFinite(packet.pitch())) return;
        lastSequence=packet.sequence();inputAge=0;
        if(packet.command()==HazeProtocol.SLIDE_STATE_COMMAND) {
            if(active)input=new Controls((input.held()&~Controls.SLIDING)|(packet.held()&Controls.SLIDING),
                input.pressed(),input.yaw(),input.pitch());
            return;
        }
        if(packet.command()==1) toggle(p);
        if(active && packet.command()>=10 && packet.command()<14) abilities.upgrade(packet.command()-10);
        input=new Controls(packet.held()&0x3ffff,packet.pressed()&0x1ffff,packet.yaw(),Math.max(-89,Math.min(89,packet.pitch())));
    }
    public void tick(ServerPlayer p) {
        if(!active) return;
        if(dimension!=p.level().dimension()) { var old=p.level().getServer().getLevel(dimension);if(old!=null)clearStatuses(old);targets.clear();dimension=p.level().dimension(); }
        if(!p.isAlive() || p.isSpectator()) { toggle(p);return; }
        if(++inputAge>5) input=Controls.NONE;
        p.fallDistance=0;p.getFoodData().setFoodLevel(20);
        double dt=0.05;
        traces.removeIf(trace->abilities.time-trace.time>0.5);
        double before=abilities.time;
        if(input.edge(Controls.DAGGER) && abilities.cast(0)) {daggerStarted=abilities.time;setAction("dagger");}
        if(input.edge(Controls.SMOKE) && abilities.cast(1)) {
            setAction("smoke");
            if(abilities.p(1,"DispelOnUse")>0){
                for(var effect:new ArrayList<>(p.getActiveEffects()))if(effect.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.HARMFUL)p.removeEffect(effect.getEffect());
                p.clearFire();
            }
        }
        if(input.edge(Controls.FLURRY)) {
            if(abilities.flurry()) abilities.flurryUntil=abilities.time;
            else if(abilities.cast(3)) setAction("flurry");
        }
        if(input.edge(Controls.PARRY) && abilities.time>=parryReady) {
            parryReady=abilities.time+DATA.prop("citadel_ability_melee_parry","AbilityCooldown",0);
            parryUntil=abilities.time+DATA.field("citadel_ability_melee_parry","m_flActiveTime");setAction("parry");
        }
        if(input.down(Controls.MELEE)) meleeHeld+=dt;
        else if(meleeHeld>0 && abilities.time>=meleeReady) { melee(p,meleeHeld>=DATA.field("ability_melee_haze","m_flHeavyAttackRequiredHoldTime"));meleeHeld=0; }
        for(int sub=0;sub<6;sub++) {
            abilities.advance(dt/6);
            if(abilities.consumeDagger()) launch(p,true,null);
            boolean flurry=abilities.flurry();
            double rate=flurry?abilities.p(3,"BonusFireRate"):0;
            double bonus=flurry?abilities.p(3,"WeaponDamageBonus"):0;
            if(input.edge(Controls.RELOAD)) { gun.reload(0);setAction("reload"); }
            boolean fire=input.down(Controls.FIRE) && abilities.daggerAt<0 && meleeHeld<=0 && abilities.time>=parryUntil;
            boolean slide=input.down(Controls.SLIDING)&&input.down(Controls.CROUCH);
            for(Gun.Shot shot:gun.advance(dt/6,fire||flurry,input.down(Controls.AIM),flurry,slide,rate,bonus,0)) {
                if(gun.shotCount%2==0)lastLeftShot=abilities.time;else lastRightShot=abilities.time;
                if(fire) abilities.breakSmoke();
                if(flurry) flurryShot(p,shot);
                else launch(p,false,shot);
                if(!flurry) setAction("fire");
            }
            advanceProjectiles(p,dt/6);
        }
        p.setInvisible(abilities.invisible());
        if(p.getHealth()<p.getMaxHealth()) p.heal((float)(DATA.stat("EBaseHealthRegen")*dt));
        applyStatuses(p);
        actionTime+=abilities.time-before;
        input=input.withoutEdges();
    }
    private void setAction(String next) { action=next;actionTime=0; }
    private void launch(ServerPlayer p,boolean dagger,Gun.Shot shot) {
        SkinnedAsset skeleton=SkinnedAsset.shared();String prefix="models/heroes_staging/haze/clips/";
        String clip=prefix+(input.down(Controls.CROUCH)?"weapon_crouch_idle":"weapon_stand_idle");
        double pitch=input.pitch()-gun.recoil;
        var layers=new ArrayList<SkinnedAsset.Layer>();layers.add(AnimationSource.aim(skeleton,"haze","idle",pitch,0));
        if(dagger)layers.add(AnimationSource.dagger(abilities.time-daggerStarted));
        var pose=skeleton.pose(clip,abilities.time,true,layers);
        V3 pos=of(p.position()).add(SkinnedAsset.worldOffset(skeleton.attachment(pose,dagger?"palm_R":gun.shotCount%2==0?"muzzle_flash_l":"muzzle_flash_r"),input.yaw()));
        JsonObject w=dagger?DATA.ability("ability_sleep_dagger").getAsJsonObject("m_mapWeaponInfos").getAsJsonObject("primary"):DATA.weapon();
        double speed=Retail.number(w.get("m_flBulletSpeed"))*Retail.SOURCE_UNIT;
        V3 direction=aimAt(p,pitch).sub(pos).normalized();
        double yaw=Math.toDegrees(Math.atan2(-direction.x(),direction.z()))+(shot==null?0:shot.yawOffset());
        double elevation=-Math.toDegrees(Math.asin(direction.y()))+(shot==null?0:shot.pitchOffset());
        V3 velocity=V3.look(yaw,elevation).mul(speed);
        double life=w.has("m_flBulletLifetime")?Retail.number(w.get("m_flBulletLifetime")):DATA.weapon().get("m_flBulletLifetime").getAsDouble();
        long id=nextProjectile++;
        projectiles.add(new Projectile(pos,velocity,pos,life,dagger,shot==null?abilities.p(0,"Damage"):shot.damage(),id));
        traces.add(new Trace(id,abilities.time,pos,pos,dagger));
    }
    private V3 aimAt(ServerPlayer p,double pitch){
        V3 forward=V3.look(input.yaw(),pitch),right=V3.look(input.yaw()+90,0);
        double height=DATA.camera.get(input.down(Controls.CROUCH)?"m_flCameraHeightCrouching":"m_flCameraHeightStanding").getAsDouble()*Retail.SOURCE_UNIT;
        double back=DATA.camera.get(input.down(Controls.AIM)?"m_flCameraBackOffsetAiming":"m_flCameraBackOffset").getAsDouble()*Retail.SOURCE_UNIT;
        V3 pivot=of(p.position()).add(new V3(0,height,0));
        V3 camera=pivot.sub(forward.mul(back)).sub(right.mul(DATA.camera.get("m_flCameraSideOffset").getAsDouble()*Retail.SOURCE_UNIT));
        Vec3 from=mc(camera),to=mc(camera.add(forward.mul(gun.f("m_flRange")*Retail.SOURCE_UNIT)));
        BlockHitResult wall=p.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        Vec3 target=wall.getType()==HitResult.Type.MISS?to:wall.getLocation();double nearest=from.distanceToSqr(target);
        for(LivingEntity e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,target).inflate(1),e->e!=p&&e.isAlive())){
            var hit=e.getBoundingBox().clip(from,target);if(hit.isPresent()&&from.distanceToSqr(hit.get())<nearest){target=hit.get();nearest=from.distanceToSqr(target);}
        }
        return of(target);
    }
    private void advanceProjectiles(ServerPlayer p,double dt) {
        ServerLevel level=p.level();List<Projectile> alive=new ArrayList<>();
        for(Projectile shot:projectiles) {
            JsonObject w=shot.dagger?DATA.ability("ability_sleep_dagger").getAsJsonObject("m_mapWeaponInfos").getAsJsonObject("primary"):DATA.weapon();
            double gravity=w.has("m_flBulletGravityScale")?Retail.number(w.get("m_flBulletGravityScale")):0;
            V3 accel=new V3(0,-DATA.nativeFloat("sv_gravity")*Retail.SOURCE_UNIT*gravity,0);
            V3 end=shot.position.add(shot.velocity.mul(dt)).add(accel.mul(dt*dt*0.5));
            Vec3 from=mc(shot.position),to=mc(end);
            BlockHitResult block=level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
            double nearest=block.getType()==HitResult.Type.MISS?Double.POSITIVE_INFINITY:from.distanceToSqr(block.getLocation());
            LivingEntity victim=null;Vec3 hit=null;
            double radius=Retail.number(w.get("m_flBulletRadius"))*Retail.SOURCE_UNIT;
            AABB bounds=new AABB(from,to).inflate(radius);
            for(LivingEntity entity:level.getEntitiesOfClass(LivingEntity.class,bounds,e->e!=p && e.isAlive() && !(e instanceof Player))) {
                Optional<Vec3> intersect=entity.getBoundingBox().inflate(radius).clip(from,to);
                if(entity.getBoundingBox().inflate(radius).contains(from)) intersect=Optional.of(from);
                if(intersect.isPresent() && from.distanceToSqr(intersect.get())<nearest) {
                    nearest=from.distanceToSqr(intersect.get());victim=entity;hit=intersect.get();
                }
            }
            V3 visualEnd=victim!=null?of(hit):nearest==Double.POSITIVE_INFINITY?end:of(block.getLocation());
            for(int i=0;i<traces.size();i++)if(traces.get(i).id==shot.id){Trace old=traces.get(i);traces.set(i,new Trace(old.id,old.time,old.start,visualEnd,old.dagger));break;}
            if(victim!=null) impact(p,victim,shot,hit);
            else if(nearest==Double.POSITIVE_INFINITY && shot.remaining>dt && end.sub(shot.origin).length()<DATA.weapon().get("m_flRange").getAsDouble()*Retail.SOURCE_UNIT)
                alive.add(new Projectile(end,shot.velocity.add(accel.mul(dt)),shot.origin,shot.remaining-dt,shot.dagger,shot.damage,shot.id));
        }
        projectiles.clear();projectiles.addAll(alive);
    }
    private void impact(ServerPlayer p,LivingEntity victim,Projectile projectile,Vec3 hit) {
        Fixation f=targets.computeIfAbsent(victim.getId(),id->new Fixation());
        double damage=projectile.damage;
        if(projectile.dagger) {
            hurt(p,victim,damage,false);
            f.dagger(DATA,abilities.ranks[0],abilities.ranks[2],abilities.time);
        } else {
            f.advance(DATA,abilities.time);
            boolean head=hit.y>=victim.getY()+victim.getBbHeight()*0.8;
            Fixation.Hit stack=f.bullet(DATA,abilities.ranks[2],head,abilities.time);
            damage=(damage+stack.extraBulletDamage())*gun.damageAt(projectile.position.sub(projectile.origin).length());
            if(abilities.time<f.bulletResistUntil) damage*=1-f.resistReduction/100;
            f.damaged(DATA,abilities.ranks[0],abilities.time);
            hurt(p,victim,damage,false);
            if(stack.spiritProc()>0) hurt(p,victim,stack.spiritProc(),true);
            if(abilities.time<abilities.buffUntil) p.heal((float)(damage*abilities.p(1,"BulletLifesteal")/100));
        }
    }
    private void hurt(ServerPlayer p,LivingEntity e,double damage,boolean spirit) {
        combat(e);
        DamageSource source=new DamageSource(p.level().registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(BULLET),p,p);
        e.hurtServer(p.level(),source,(float)damage);
    }
    public void combat(Entity other){
        String field=other instanceof Player?"m_flInCombatWithHeroDuration":other instanceof net.minecraft.world.entity.monster.Monster?"m_flInCombatWithNonHeroDuration":"m_flInCombatWithNeutralDuration";
        combatUntil=Math.max(combatUntil,abilities.time+Retail.number(DATA.root.getAsJsonObject("genericData").get(field)));
    }
    public boolean inCombat(){return abilities.time<combatUntil;}
    private void flurryShot(ServerPlayer p,Gun.Shot shot) {
        double radius=abilities.p(3,"Radius"),min=abilities.p(3,"RadiusMin");
        List<LivingEntity> candidates=p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius),e->e!=p && e.isAlive() && !(e instanceof Player));
        candidates.removeIf(e->p.distanceTo(e)>radius || p.distanceTo(e)<min || !p.hasLineOfSight(e));
        candidates.sort(Comparator.comparingDouble(p::distanceToSqr));
        if(candidates.isEmpty()) return;
        int limit=(int)abilities.p(3,"TargetsPerTick");
        for(int i=0;i<Math.min(limit,candidates.size());i++) {
            LivingEntity e=candidates.get((int)(gun.shotCount+i)%candidates.size());
            V3 origin=of(p.position()).add(new V3(0,1.5,0));
            V3 target=of(e.position()).add(new V3(0,e.getBbHeight()/2,0));
            V3 velocity=target.sub(origin).normalized().mul(gun.f("m_flBulletSpeed")*Retail.SOURCE_UNIT);
            long id=nextProjectile++;projectiles.add(new Projectile(origin,velocity,origin,gun.f("m_flBulletLifetime"),false,shot.damage(),id));
            traces.add(new Trace(id,abilities.time,origin,origin,false));
        }
    }
    private void melee(ServerPlayer p,boolean heavy) {
        abilities.breakSmoke();setAction(heavy?"melee_heavy":"melee_light");
        double range=DATA.prop("ability_melee_haze","MeleeAttackLength",0);
        V3 look=V3.look(input.yaw(),0);
        double cos=Math.cos(Math.toRadians(DATA.prop("ability_melee_haze","MeleeHalfAngle",0)));boolean hit=false;
        for(LivingEntity e:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range),e->e!=p && e.isAlive() && !(e instanceof Player))) {
            if(p.distanceTo(e)<=range && look.dot(of(e.position()).sub(of(p.position())).flat().normalized())>=cos && p.hasLineOfSight(e)) {
                hurt(p,e,DATA.stat(heavy?"EHeavyMeleeDamage":"ELightMeleeDamage"),false);
                hit=true;
                if(heavy) e.setDeltaMovement(e.getDeltaMovement().add(look.x(),0,look.z()));
            }
        }
        JsonObject attack=DATA.ability("ability_melee_haze").getAsJsonObject("m_mapAttacks").getAsJsonObject(heavy?"EAttackType_Heavy":"EAttackType_Light");
        meleeReady=abilities.time+Retail.number(attack.get(hit?"m_flCooldownOnHit":"m_flCooldownOnMiss"));
    }
    private void applyStatuses(ServerPlayer p) {
        Iterator<Map.Entry<Integer,Fixation>> it=targets.entrySet().iterator();
        while(it.hasNext()) {
            var entry=it.next();Fixation f=entry.getValue();f.advance(DATA,abilities.time);
            Entity entity=p.level().getEntity(entry.getKey());
            if(!(entity instanceof Mob mob) || !mob.isAlive()) { it.remove();continue; }
            if(f.asleep(abilities.time)) {
                mob.getNavigation().stop();mob.setTarget(null);mob.setDeltaMovement(0,mob.getDeltaMovement().y,0);
            }
            var speed=mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if(speed!=null) {
                Identifier id=slowId();double slow=f.movementSlow(DATA,abilities.ranks[2],abilities.time)/100;
                var previous=speed.getModifier(id);
                if(previous!=null&&Math.abs(previous.amount()+slow)>1e-8) speed.removeModifier(id);
                if(slow>0&&!speed.hasModifier(id))speed.addTransientModifier(new AttributeModifier(id,-slow,AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
                if(slow<=0)speed.removeModifier(id);
            }
            if(!f.live(abilities.time)) it.remove();
        }
    }
    private Identifier slowId(){return Identifier.fromNamespaceAndPath("haze","slow_"+owner);}
    public void clearStatuses(ServerLevel level) {
        for(int id:targets.keySet())if(level.getEntity(id) instanceof LivingEntity e){var speed=e.getAttribute(Attributes.MOVEMENT_SPEED);if(speed!=null)speed.removeModifier(slowId());}
    }
    public boolean parrying() { return active && abilities.time<parryUntil; }
    public void parry(Mob attacker) {
        attacker.getNavigation().stop();attacker.setTarget(null);
        Fixation f=targets.computeIfAbsent(attacker.getId(),i->new Fixation());
        f.sleepUntil=abilities.time+DATA.prop("citadel_ability_melee_parry","ParriedStunTime",0);f.wakeAt=Double.POSITIVE_INFINITY;
        setAction("parry_success");
    }
    public String snapshot(ServerPlayer p) {
        JsonObject j=new JsonObject();j.addProperty("uuid",owner.toString());j.addProperty("id",p.getId());j.addProperty("active",active);
        j.addProperty("health",p.getHealth());j.addProperty("maxHealth",p.getMaxHealth());j.addProperty("ammo",gun.ammo);
        j.addProperty("reload",gun.reloadRemaining);j.addProperty("time",abilities.time);j.addProperty("recoil",gun.recoil);j.addProperty("bloom",gun.bloom);
        j.addProperty("smoke",abilities.smokeActive());j.addProperty("invisible",abilities.invisible());j.addProperty("flurry",abilities.flurry());
        j.addProperty("action",action);j.addProperty("actionTime",actionTime);j.addProperty("shots",gun.shotCount);
        j.addProperty("daggerTime",abilities.time-daggerStarted);
        j.addProperty("leftShot",lastLeftShot);j.addProperty("rightShot",lastRightShot);
        j.addProperty("inCombat",inCombat());
        j.addProperty("smokeRemaining",Math.max(0,abilities.smokeUntil-abilities.time));j.addProperty("danceRemaining",Math.max(0,abilities.flurryUntil-abilities.time));j.addProperty("smokeCharges",abilities.smokeCharges);
        JsonArray ranks=new JsonArray(),cooldowns=new JsonArray();
        for(int i=0;i<4;i++) { ranks.add(abilities.ranks[i]);cooldowns.add(abilities.cooldown(i)); }
        j.add("ranks",ranks);j.add("cooldowns",cooldowns);
        JsonArray stacks=new JsonArray();for(var entry:targets.entrySet()) {
            JsonObject s=new JsonObject();s.addProperty("id",entry.getKey());s.addProperty("count",entry.getValue().stacks);s.addProperty("sleep",entry.getValue().asleep(abilities.time));stacks.add(s);
        }j.add("stacks",stacks);
        JsonArray bullets=new JsonArray();for(Projectile p0:projectiles) {
            JsonArray b=new JsonArray();b.add(p0.position.x());b.add(p0.position.y());b.add(p0.position.z());b.add(p0.velocity.x());b.add(p0.velocity.y());b.add(p0.velocity.z());b.add(p0.dagger?1:0);bullets.add(b);
            if(bullets.size()>=128) break;
        }j.add("bullets",bullets);
        JsonArray fx=new JsonArray();for(Trace trace:traces){JsonArray line=new JsonArray();line.add(trace.id);line.add(trace.time);line.add(trace.start.x());line.add(trace.start.y());line.add(trace.start.z());line.add(trace.end.x());line.add(trace.end.y());line.add(trace.end.z());line.add(trace.dagger?1:0);fx.add(line);if(fx.size()>=32)break;}j.add("fx",fx);return j.toString();
    }
    public static V3 of(Vec3 v) { return new V3(v.x,v.y,v.z); }
    public static Vec3 mc(V3 v) { return new Vec3(v.x(),v.y(),v.z()); }
}
