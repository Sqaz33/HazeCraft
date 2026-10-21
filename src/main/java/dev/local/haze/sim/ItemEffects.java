package dev.local.haze.sim;

import java.util.*;

/** Stateful item rules; all magnitudes come from the pinned owned-game catalogue. */
public final class ItemEffects {
    public interface Host {
        double health(); double maxHealth(); void heal(double n); void drain(double n);
        int target(double range); List<Integer> nearby(double range,int center);
        default UnitRole role(int target){return target==0?UnitRole.SELF:UnitRole.ENEMY_HERO;}
        default int ally(double range){return 0;}default List<Integer> allies(double range,int center){return List.of(0);}
        default void stealHealth(int target,double amount,double duration){}
        default void healTarget(int target,double n){if(target==0)heal(n);}default void shieldTarget(int target,double n,double duration){}
        default void buffTarget(int target,ItemCatalog.Item item,double duration){}default void pull(int target){}
        default boolean enemyHeroDamage(){return true;}default void replenishCharge(){}
        default int ability(){return -1;}default void echo(int ability){refresh(false);}default void reduceCooldown(double seconds,double percent){}
        default double spirit(){return 0;}default double weapon(){return 0;}
        double targetHealth(int target); void damage(int target,double amount); void debuff(int target,ItemCatalog.Item item,double duration);
        default boolean inCombat(){return true;}default boolean behind(int target){return false;}default int point(double range){return -1;}
        void reload(double percent); void refresh(boolean ultimate); void teleport(double range,int target); void impulse(double speed); void cleanse();
    }
    public final ItemInventory inventory;private double spirit,weapon;
    private void context(Host h){spirit=h.spirit();weapon=h.weapon();}
    private double value(ItemCatalog.Item i,String key){return i.scaled(key,spirit,weapon);}
    public double staminaRestore;
    public double time,barrier,barrierUntil,invisibleUntil,flightUntil,flightSpeed,moveImpulse;
    public long movementSerial,staminaSerial;
    public final Map<String,Double> buffs=new HashMap<>();
    public final Map<String,Integer> stacks=new HashMap<>();
    private final Map<String,DamageWindow> damageWindows=new HashMap<>();private double lastDamage=-100,spellParryUntil;
    private final Map<String,Double> procs=new HashMap<>(), accum=new HashMap<>();
    private final List<RunnableAt> delayed=new ArrayList<>();
    private final List<Zone> zones=new ArrayList<>();
    private double lastShot=-100,shootStart,etherealUntil,leapUntil;
    private static final class Shield {double amount,until;Shield(double amount,double until){this.amount=amount;this.until=until;}}
    private final Map<String,Shield> shields=new LinkedHashMap<>();
    private final Map<String,Double> expirations=new HashMap<>();
    private final Random random=new Random(25950029);
    private record RunnableAt(double at,Runnable run){}
    private record Zone(ItemCatalog.Item item,int center,double until,double started){}
    public ItemEffects(ItemInventory inventory){this.inventory=inventory;}
    public boolean buff(String id){return inventory.owned.contains(id)&&buffs.getOrDefault(id,0D)>time;}
    public double cooldown(String id){return Math.max(0,inventory.readyAt.getOrDefault(id,0D)-time);}
    public boolean proc(ItemCatalog.Item i,double cooldown){String id=i.id();if(procs.getOrDefault(id,0D)>time)return false;procs.put(id,time+Math.max(0,cooldown));return true;}
    private double duration(ItemCatalog.Item i){for(String k:List.of("AbilityDuration","BuffDuration","CasterBuffDuration","BarrierDuration","BonusShotsDuration","RegenDuration"))if(value(i,k)>0)return value(i,k);return 0;}
    public void activateBuff(ItemCatalog.Item i,double duration){buffs.put(i.id(),time+duration);}
    public void shield(double amount,double duration){shield("manual",amount,duration);}
    public void shield(String id,double amount,double duration){if(amount<=0||duration<=0)return;shields.put(id,new Shield(amount,time+duration));updateShields();}
    private void updateShields(){shields.entrySet().removeIf(e->e.getValue().until<=time||!e.getKey().equals("manual")&&!inventory.owned.contains(e.getKey()));barrier=shields.values().stream().mapToDouble(s->s.amount).sum();barrierUntil=shields.values().stream().mapToDouble(s->s.until).max().orElse(time);}
    public void stop(){buffs.clear();delayed.clear();zones.clear();shields.clear();barrier=0;invisibleUntil=flightUntil=leapUntil=etherealUntil=0;}
    public boolean busy(){return buff("upgrade_self_bubble")&&time<etherealUntil;}

    public boolean use(int slot,Host h){
        context(h);var active=inventory.active();if(slot<0||slot>=active.size())return false;
        var i=ItemCatalog.ITEMS.get(active.get(slot));String id=i.id();
        if(id.equals("upgrade_blood_tribute")&&buff(id)){buffs.remove(id);return true;}
        if(id.equals("upgrade_rocket_booster")&&leapUntil>time){moveImpulse=-value(i,"DropDownSpeed");movementSerial++;leapUntil=0;return true;}
        if(id.equals("upgrade_rocket_booster")&&time-lastDamage<value(i,"InterruptCooldown"))return false;
        if(cooldown(id)>0)return false;
        boolean enemy=Set.of("upgrade_non_player_bonus_sacrifice","upgrade_containment","upgrade_spirit_sap","upgrade_focus_lens","upgrade_withering_whip","upgrade_greater_withering_whip","upgrade_rupture","upgrade_target_stun","upgrade_phantom_strike","upgrade_glitch").contains(id);
        boolean friendly=i.targeting().contains("HERO_FRIENDLY")&&i.data().get("m_eAbilityTargetingLocation").getAsString().contains("UNIT");
        int target=enemy?h.target(value(i,"AbilityCastRange")):friendly?h.ally(value(i,"AbilityCastRange")):0;
        if(enemy&&target>=0&&!i.accepts(h.role(target))){inventory.message="Недопустимый тип цели";return false;}
        if(enemy&&target<0){inventory.message="Нет цели в пределах дальности";return false;}
        inventory.readyAt.put(id,time+value(i,"AbilityCooldown")*(1-inventory.reduction("ItemCooldownReduction")));
        inventory.message=i.name();
        double delay=Math.max(0,value(i,"AbilityCastDelay"));
        if(delay>0)delayed.add(new RunnableAt(time+delay,()->execute(i,target,h)));else execute(i,target,h);
        return true;
    }
    private void execute(ItemCatalog.Item i,int target,Host h){
        String id=i.id();double d=duration(i);activateBuff(i,d);
        switch(id){
            case "upgrade_non_player_bonus_sacrifice" -> {h.damage(target,h.targetHealth(target)*10);activateBuff(i,d);}
            case "upgrade_capacitor" -> {for(int t:h.nearby(value(i,"ChainRadius"),0)){h.damage(t,value(i,"Damage"));h.debuff(t,i,value(i,"SlowDuration"));}}
            case "upgrade_targeted_silence" -> {for(int t:h.nearby(value(i,"AbilityCastRange"),-2)){h.damage(t,value(i,"Damage"));h.debuff(t,i,d);}}
            case "upgrade_reduce_debuff_duration" -> {h.cleanse();h.heal(value(i,"HealOnActivate"));}
            case "upgrade_grit","upgrade_guardian_ward","upgrade_divine_barrier" -> {double amount=Math.max(value(i,"CombatBarrier"),value(i,"GuardianWardCombatBarrier"));if(target==0)shield(i.id(),amount,d);else{h.shieldTarget(target,amount,d);h.buffTarget(target,i,d);buffs.remove(id);}}
            case "upgrade_containment","upgrade_spirit_sap","upgrade_focus_lens","upgrade_withering_whip","upgrade_greater_withering_whip","upgrade_glitch" -> h.debuff(target,i,d);
            case "upgrade_rupture" -> {h.debuff(target,i,d);zones.add(new Zone(i,target,time+d,time));}
            case "upgrade_discord" -> {zones.add(new Zone(i,target,time+d,time));h.buffTarget(target,i,d);}
            case "upgrade_target_stun" -> delayed.add(new RunnableAt(time+value(i,"StunDelay"),()->h.debuff(target,i,value(i,"StunDuration"))));
            case "upgrade_phantom_strike" -> {h.teleport(value(i,"AbilityCastRange"),target);h.damage(target,value(i,"ImpactDamage"));h.debuff(target,i,value(i,"SlowDuration"));}
            case "upgrade_warp_stone" -> h.teleport(value(i,"AbilityCastRange"),-1);
            case "upgrade_aoe_root" -> {for(int t:h.nearby(value(i,"CaptureRadius"),h.point(value(i,"AbilityCastRange"))))h.debuff(t,i,d);}
            case "upgrade_ability_refresher" -> h.refresh(true);
            case "upgrade_ability_power_shard" -> h.echo(inventory.bound(i.id()));
            case "upgrade_cloaking_device_active" -> invisibleUntil=time+d;
            case "upgrade_rocket_booster" -> {leapUntil=time+value(i,"JumpVelocityHidden");moveImpulse=value(i,"JumpVelocityHidden");movementSerial++;h.impulse(moveImpulse);shield(i.id(),value(i,"CombatBarrier"),value(i,"BarrierDuration"));}
            case "upgrade_health_nova","upgrade_health_stimpak" -> zones.add(new Zone(i,target,time+value(i,"RegenDuration"),time));
            case "upgrade_restorative_locket" -> {int count=stacks.getOrDefault(id,0);h.healTarget(target,value(i,"HealPerStack")*count);staminaRestore=Math.max(value(i,"MinStaminaRestore"),value(i,"MaxStaminaRestore")*count/Math.max(1,value(i,"MaxStacks")));stacks.put(id,0);staminaSerial++;}
            case "upgrade_rescue_beam" -> zones.add(new Zone(i,target,time+value(i,"AbilityChannelTime"),time));
            case "upgrade_thermal_detonator" -> zones.add(new Zone(i,h.point(value(i,"Radius")),time+d,time));
            case "upgrade_blood_tribute" -> activateBuff(i,Double.MAX_VALUE);
            case "upgrade_unstoppable" -> h.cleanse();
            case "upgrade_cold_front","upgrade_arctic_blast" -> {for(int t:h.nearby(value(i,"EndRadius"),0)){h.damage(t,value(i,"Damage"));h.debuff(t,i,Math.max(d,value(i,"SlowDuration")));}}
            case "upgrade_self_bubble" -> {etherealUntil=time+d;h.cleanse();delayed.add(new RunnableAt(time+d,()->{activateBuff(i,value(i,"BuffDuration"));h.reload(100);}));}
            case "upgrade_surging_power" -> h.reload(value(i,"ActiveReloadPercent"));
            case "upgrade_magic_carpet" -> {flightUntil=time+value(i,"SummonDuration")+d;flightSpeed=value(i,"FlyMoveSpeed");}
            case "upgrade_goose_egg" -> inventory.message="Души уже бесконечны";
            default -> { /* Self buffs are applied by bonus(), immunity(), and onBullet(). */ }
        }
    }
    public void tick(double dt,Host h){
        context(h);double previous=time;time+=dt;updateShields();
        var tasks=new ArrayList<>(delayed);delayed.clear();for(var task:tasks)if(task.at<=time)task.run.run();else delayed.add(task);
        buffs.keySet().removeIf(id->!inventory.owned.contains(id)||buffs.get(id)<=time);
        for(var z:new ArrayList<>(zones)){
            if(z.until<=previous||!inventory.owned.contains(z.item.id())){zones.remove(z);continue;}
            var i=z.item;double elapsed=Math.min(time,z.until)-Math.max(previous,z.started);
            if(value(i,"TotalHealthRegen")>0){var recipients=i.id().equals("upgrade_health_nova")?h.allies(value(i,"AuraRadius"),z.center):List.of(z.center);for(int ally:recipients)h.healTarget(ally,value(i,"TotalHealthRegen")/value(i,"RegenDuration")*elapsed);continue;}
            if(i.id().equals("upgrade_rescue_beam")){h.healTarget(z.center,h.targetHealth(z.center)*value(i,"HealPercentAmount")/100*elapsed/value(i,"AbilityChannelTime"));if(z.center!=0)h.heal(h.maxHealth()*value(i,"HealPercentAmount")/100*elapsed/value(i,"AbilityChannelTime"));continue;}
            double radius=Math.max(value(i,"Radius"),value(i,"AuraRadius"));
            List<Integer> targets=z.center>0&&(i.id().equals("upgrade_rupture")||value(i,"DotDuration")>0||i.id().equals("upgrade_weapon_backstabber"))?List.of(z.center):h.nearby(radius,z.center);
            for(int t:targets){double damage=value(i,"DPS");
                damage+=h.targetHealth(t)*(value(i,"MaxHealthPercentAsDPS")+value(i,"DotHealthPercent"))/100;
                if(value(i,"DPSIncrease")>0)damage=Math.min(value(i,"DPSMax"),damage+(time-z.started)*value(i,"DPSIncrease"));
                h.damage(t,damage*elapsed);h.debuff(t,i,.2);
            }
        }
        if(buff("upgrade_blood_tribute")){var i=ItemCatalog.ITEMS.get("upgrade_blood_tribute");if(h.health()<=value(i,"HealthDrainedPerSecond")*dt+1)buffs.remove(i.id());else h.drain(value(i,"HealthDrainedPerSecond")*dt);}
        for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);
            if(id.equals("upgrade_fervor")&&h.health()/h.maxHealth()*100<value(i,"LowHealthThreshold")&&proc(i,value(i,"AbilityCooldown")))activateBuff(i,value(i,"AbilityDuration"));
            if(buff(id)&&value(i,"Regeneration")>0)h.heal(value(i,"Regeneration")*dt);
            if(time-lastDamage>=value(i,"RestoreDelay")&&value(i,"HealLifePercentOutOfCombat")>0)h.heal(h.maxHealth()*value(i,"HealLifePercentOutOfCombat")/100*dt);
            if(expirations.getOrDefault(id,Double.MAX_VALUE)<=time){stacks.remove(id);accum.remove(id);expirations.remove(id);}
            if(id.equals("upgrade_tech_damage_pulse")&&proc(i,value(i,"AbilityCooldown")*(1-inventory.reduction("ItemCooldownReduction")))){int count=0;for(int t:h.nearby(value(i,"DamagePulseRadius"),0))if(count++<i.field("m_iMaxTargets"))h.damage(t,value(i,"DamagePulseAmount"));}
            if(id.equals("upgrade_bullet_armor_reduction_aura")||id.equals("upgrade_juggernaut")||buff(id)&&id.equals("upgrade_colossus"))for(int t:h.nearby(value(i,"Radius")>0?value(i,"Radius"):8,0))h.debuff(t,i,.2);
        }
    }
    public double bonus(String key,double healthPercent){
        double value=0;
        for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);boolean active=buff(id);
            if(active&&!(id.equals("upgrade_self_bubble")&&busy())){value+=switch(key){
                case "BonusMoveSpeed" -> (id.equals("upgrade_cloaking_device_active")&&invisibleUntil>time?value(i,"InvisMoveSpeedMod"):0)+(id.equals("upgrade_counterspell")||id.equals("upgrade_weapon_backstabber")||id.equals("upgrade_resonant_healing")||id.equals("upgrade_magic_storm")?value(i,"BonusMoveSpeed"):0)+value(i,"ActiveBonusMoveSpeed")+value(i,"ActiveMoveSpeedPenalty")+(id.equals("upgrade_blood_tribute")||id.equals("upgrade_guardian_ward")||id.equals("upgrade_divine_barrier")?value(i,"BonusMoveSpeed"):0);
                case "BonusFireRate" -> (id.equals("upgrade_cloaking_device_active")&&invisibleUntil<=time?value(i,"AmbushBonusFireRate"):0)+value(i,"ActiveBonusFireRate")+(Set.of("upgrade_dps_aura","upgrade_blood_tribute","upgrade_active_reload","upgrade_kinetic_sash","upgrade_tech_overflow","upgrade_healbuff").contains(id)?value(i,"BonusFireRate"):0)+value(i,"ActivatedFireRate")+value(i,"FireRateBonus");
                case "SpiritPower" -> (id.equals("upgrade_spirit_snatch")?value(i,"TechPowerGain"):0)+(id.equals("upgrade_cloaking_device_active")&&invisibleUntil<=time?value(i,"AmbushBonusTechPower"):0)+value(i,"BonusSpirit")+(id.equals("upgrade_arcane_surge")?value(i,"SpiritPower"):0);
                case "BulletLifestealPercent" -> value(i,"ActiveBonusLifesteal")+(id.equals("upgrade_active_reload")?value(i,"BulletLifestealPercent"):0);
                case "AbilityLifestealPercentHero" -> id.equals("upgrade_infuser")?value(i,key):value(i,"BonusSpiritLifesteal");
                case "BulletResist" -> value(i,"BuffBulletResist")+(id.equals("upgrade_warp_stone")?value(i,"BulletResist"):0);
                case "TechResist" -> value(i,"BuffTechResist")+(id.equals("upgrade_spirit_snatch")?value(i,"TechArmorGain"):0);
                case "BonusMeleeDamagePercent" -> id.equals("upgrade_colossus")?value(i,key):id.equals("upgrade_cloaking_device_active")&&invisibleUntil<=time?value(i,"AmbushBonusMeleeDamage"):0;
                case "BonusClipSize" -> id.equals("upgrade_kinetic_sash")?value(i,key):0;
                default -> 0;};}
            if(value(i,"LifeThreshold")>0&&healthPercent>=value(i,"LifeThreshold")&&!i.stats().containsKey(key))value+=value(i,key);
            if(id.equals("upgrade_chonky")&&healthPercent>=value(i,"HealthThreshold")&&key.equals("BonusMoveSpeed"))value+=value(i,key);
            if((id.equals("upgrade_tech_purge")&&key.equals("TechResist")||id.equals("upgrade_improved_bullet_armor")&&key.equals("BulletResist"))&&!i.stats().containsKey(key))value+=value(i,key);
            if(id.equals("upgrade_intensifying_clip")&&key.equals("BaseAttackDamagePercent")&&time-lastShot<.3)value+=value(i,"BaseAttackDamagePercentAtMaxDuration")*Math.min(1,(time-shootStart)/value(i,"ShootDurationForMax"));
            if(id.equals("upgrade_reinforcing_casings")&&key.equals("BulletResist"))value+=stacks.getOrDefault(id,0)*value(i,"BulletResistPerStack");
            if(id.equals("upgrade_trophy_collector"))value+=stacks.getOrDefault(id,0)*value(i,"Stacking"+key);
            
            if(active&&id.equals("upgrade_headhunter")&&key.equals("BonusMoveSpeed"))value+=value(i,key);
            if(active&&id.equals("upgrade_melee_rebuttal")&&key.equals("BonusMeleeDamagePercent"))value+=value(i,"BonusDamagePercent");
            if(value(i,"HealthThreshold")>0&&healthPercent<value(i,"HealthThreshold"))value+=value(i,key+"BelowThreshold");
            if(id.equals("upgrade_fervor")&&active)value+=key.equals("BonusFireRate")?value(i,"FervorFireRate"):key.equals("BonusMoveSpeed")?value(i,"FervorMovespeed"):0;
            if(key.equals("BaseAttackDamagePercent"))value+=stacks.getOrDefault(id,0)*(id.equals("upgrade_bulletshredimbue")?value(i,"WeaponPowerPerStackNonHero"):value(i,"WeaponPowerPerStack"));
            if(key.equals("BonusFireRate"))value+=stacks.getOrDefault(id,0)*value(i,"FireRatePerKill");
            if(active&&id.equals("upgrade_enchanted_holsters")&&(key.equals("BonusFireRate")||key.equals("ReloadSpeedMultipler")))value+=stacks.getOrDefault(id,0)*value(i,key);
            if(active&&id.equals("upgrade_diviners_kevlar")&&key.equals("TechPower"))value+=value(i,key);
            if(active&&id.equals("upgrade_counterspell")&&key.equals("SpiritPower"))value+=value(i,key);
        }return value;
    }
    public void onShot(){if(time-lastShot>.3)shootStart=time;lastShot=time;}
    public double onBullet(int target,double damage,boolean head,double distance,double targetHealthPercent,Host h){
        context(h);for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);if(!i.accepts(h.role(target)))continue;
            if(distance<value(i,"CloseRangeBonusDamageRange"))damage*=1+value(i,"CloseRangeBonusWeaponPower")/100;
            if(value(i,"LongRangeBonusWeaponPowerMinRange")>0&&distance>=value(i,"LongRangeBonusWeaponPowerMinRange"))damage*=1+value(i,"LongRangeBonusWeaponPower")/100;
            if(value(i,"EnemyLifeThreshold")>0&&targetHealthPercent>=value(i,"EnemyLifeThreshold"))damage*=1+value(i,"BaseAttackDamagePercentBonus")/100;
            if(head&&value(i,"HeadShotBonusDamage")>0&&proc(i,value(i,"AbilityCooldown"))){damage+=value(i,"HeadShotBonusDamage");h.heal(h.maxHealth()*value(i,"HealPercentPerHeadshot")/100);activateBuff(i,value(i,"MovementSpeedBonusDuration"));}
            if(value(i,"CritDamagePercent")>0&&random.nextDouble()*100<value(i,"ProcChance"))damage*=1+value(i,"CritDamagePercent")/100;
            if(value(i,"ProcBonusMagicDamage")>0&&proc(i,value(i,"AbilityCooldown")))h.damage(target,value(i,"ProcBonusMagicDamage"));
            if(value(i,"HealFromNPC")>0&&proc(i,value(i,"AbilityCooldown")))h.heal(value(i,h.role(target).hero()?"HealFromHero":"HealFromNPC"));
            if(value(i,"RicochetTargets")>0){int n=0;for(int t:h.nearby(value(i,"RicochetRadius"),target))if(t!=target&&n++<value(i,"RicochetTargets"))h.damage(t,damage*value(i,"RicochetDamagePercent")/100);}
            if(value(i,"DamagePerChain")>0&&random.nextDouble()*100<value(i,"ProcChance")&&proc(i,value(i,"ProcCooldown"))){int n=0;for(int t:h.nearby(value(i,"ChainRadius"),target))if(n++<value(i,"ChainCount"))h.damage(t,value(i,"DamagePerChain"));}
            boolean bulletDebuff=Set.of("upgrade_hollow_point_rounds","upgrade_weighted_shots","upgrade_slowing_bullets","upgrade_inhibitor","upgrade_tech_defense_shredders","upgrade_proc_silence","upgrade_headshot_booster2","upgrade_spellslinger_headshots","upgrade_banshee_slugs","upgrade_close_quarter_combat","upgrade_toxic_bullets","upgrade_tech_overflow").contains(id);
            if(id.equals("upgrade_hollow_point_rounds")&&h.health()/h.maxHealth()*100<value(i,"LifeThreshold"))bulletDebuff=false;
            if(id.equals("upgrade_close_quarter_combat")&&distance>=value(i,"CloseRangeBonusDamageRange"))bulletDebuff=false;
            boolean buildup=bulletDebuff&&value(i,"BuildUpPerShot")>0;
            boolean trigger=!buildup;
            if(buildup){String k=id+":"+target;double a=accum.getOrDefault(k,0D)+value(i,"BuildUpPerShot");trigger=a>=value(i,"BuildUpDuration");accum.put(k,trigger?0:a);}
            boolean headOnly=id.contains("headshot")||id.equals("upgrade_banshee_slugs");
            if(bulletDebuff&&trigger&&(!headOnly||head)){
                h.debuff(target,i,Math.max(value(i,"DebuffDuration"),Math.max(value(i,"SlowDuration"),value(i,"AbilityDuration"))));
                if(value(i,"DotDuration")>0)zones.add(new Zone(i,target,time+value(i,"DotDuration"),time));
                if(id.equals("upgrade_tech_overflow"))activateBuff(i,duration(i));
            }
            if(id.equals("upgrade_burst_fire")&&proc(i,value(i,"AbilityCooldown")))activateBuff(i,duration(i));
            if(id.equals("upgrade_reinforcing_casings")){stacks.merge(id,1,(a,b)->Math.min((int)value(i,"MaxArmorStacks"),a+b));expirations.put(id,time+value(i,"BulletResistDuration"));}
            if(id.equals("upgrade_weapon_backstabber")&&h.behind(target)&&proc(i,value(i,"AbilityCooldown"))){zones.add(new Zone(i,target,time+value(i,"DebuffDuration"),time));activateBuff(i,value(i,"DebuffDuration"));}
            if(buff("upgrade_ethereal_bullets")&&id.equals("upgrade_ethereal_bullets"))h.damage(target,value(i,"BulletsBonusMagicDamage"));
            if(id.equals("upgrade_siphon_bullets")&&h.role(target).hero()&&proc(i,value(i,"ProcCooldown")))h.stealHealth(target,h.targetHealth(target)*value(i,"HealthStealPctHero")/100,value(i,"StealDuration"));
            if(id.equals("upgrade_express_shot")&&proc(i,value(i,"AbilityCooldown")))damage*=1+value(i,"ProcBaseAttackDamagePercent")/100;
            if(buff(id)&&id.equals("upgrade_split_shot")){int n=0;for(int t:h.nearby(15,target))if(t!=target&&n++<value(i,"BulletSplitShot"))h.damage(t,damage);}
        }return damage;
    }
    public void onMelee(int target,boolean heavy,Host h){context(h);for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);
        if((value(i,"SpiritDamage")>0||value(i,"LifestrikeHeal")>0||value(i,"LifestealHeal")>0)&&proc(i,value(i,"AbilityCooldown")*(heavy?1:Math.max(1,value(i,"LightMeleeCooldownMult"))))){
            if(id.equals("upgrade_spirit_snatch"))activateBuff(i,value(i,"AbilityDuration"));
            h.damage(target,value(i,"SpiritDamage"));h.heal((value(i,"LifestrikeHeal")+value(i,"LifestealHeal")+h.maxHealth()*value(i,"LifestealHealPercent")/100)*(h.role(target).hero()?1:value(i,"NonHeroHealPct")/100));h.debuff(target,i,Math.max(value(i,"AbilityDuration"),value(i,"SlowDuration")));}
        if(heavy&&value(i,"BonusHeavyMeleeDamage")>0&&proc(i,value(i,"AbilityCooldown"))){h.damage(target,value(i,"BonusHeavyMeleeDamage"));h.reload(100);
            if(id.equals("upgrade_crushing_fists")){String key=id+":"+target;int n=stacks.merge(key,(int)value(i,"HeavyMeleeMultiplier"),Integer::sum);if(n>=value(i,"MaxStacks")){stacks.put(key,0);h.debuff(target,i,value(i,"StunDuration"));}}}
    }}
    public void onCast(int index,Host h){context(h);for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);
        
        if(index==3&&id.equals("upgrade_diviners_kevlar")&&proc(i,value(i,"AbilityCooldown"))){shield(i.id(),value(i,"CombatBarrier"),value(i,"BuffDuration"));activateBuff(i,value(i,"BuffDuration"));}
        if(id.equals("upgrade_enchanted_holsters")&&h.inCombat()){stacks.merge(id,1,(a,b)->Math.min((int)value(i,"MaxStacks"),a+b));activateBuff(i,value(i,"BuffDuration"));expirations.put(id,time+value(i,"BuffDuration"));}
        if((!ItemInventory.needsImbue(i)||inventory.bound(id)==index)&&Set.of("upgrade_resonant_healing","upgrade_mystic_regeneration","upgrade_quick_silver","upgrade_ethereal_bullets","upgrade_magic_storm","upgrade_enchanted_holsters").contains(id)&&proc(i,value(i,"AbilityCooldown"))){h.heal(value(i,"HealingPerCast"));h.reload(value(i,"AmmoReloadPercent"));activateBuff(i,Math.max(duration(i),value(i,"RegenerationDuration")));}
    }}
    public void onSpirit(int target,double damage,Host h){context(h);for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);
        if(Set.of("upgrade_magic_slow","upgrade_suppressor","upgrade_healbane","upgrade_bullet_resist_shredder","upgrade_escalating_exposure").contains(id))h.debuff(target,i,Math.max(value(i,"AbilityDuration"),value(i,"DebuffDuration")));
        if(Set.of("upgrade_magic_vulnerability","upgrade_spirit_burn").contains(id))h.debuff(target,i,Math.max(8,value(i,"DebuffDuration")));
        if(Set.of("upgrade_magic_burst","upgrade_magic_shock").contains(id)&&damage>=value(i,"MinimumDamage")&&proc(i,Math.max(value(i,"AbilityCooldown"),value(i,"ReProcLockoutTime"))))h.damage(target,value(i,"Damage")+h.targetHealth(target)*value(i,"CurrentHealthDamage")/100);
        if(id.equals("upgrade_spirit_burn")){String key=id+":"+target;double a=damageWindows.computeIfAbsent(key,k->new DamageWindow()).add(time,value(i,"DamageThresholdDuration"),damage);if(a>=value(i,"DamageThreshold")&&procTarget(i,target,value(i,"ImmunityDuration"))){damageWindows.get(key).clear();h.damage(target,value(i,"ExplosionDamage"));zones.add(new Zone(i,target,time+value(i,"DebuffDuration"),time));}}
        if(id.equals("upgrade_bulletshredimbue")&&h.ability()==inventory.bound(id)){stacks.merge(id,1,(a,b)->Math.min((int)value(i,"NonHeroStackLimit"),a+b));expirations.put(id,time+value(i,"AbilityDuration"));}
        if(value(i,"ProcBonusDamage")>0&&proc(i,value(i,"AbilityCooldown")))h.damage(target,value(i,"ProcBonusDamage"));
        if(id.equals("upgrade_mystic_reverb")&&h.ability()==inventory.bound(id)&&damage>=value(i,"MinimumDamage")&&proc(i,value(i,"AbilityCooldown")))delayed.add(new RunnableAt(time+value(i,"DelayDuration"),()->{for(int t:h.nearby(value(i,"Radius"),target))h.damage(t,damage*value(i,"TechDamagePercent")/100);}));
    }}
    public double incoming(double damage,boolean bullet,boolean spirit,Host h){context(h);
        if(!bullet&&spellParryUntil>time){var counter=ItemCatalog.ITEMS.get("upgrade_counterspell");if(counter!=null&&inventory.owned.contains(counter.id())){h.heal(value(counter,"HealOnSuccess"));activateBuff(counter,value(counter,"BuffDuration"));}return 0;}
        if(buff("upgrade_self_bubble")&&time<etherealUntil||bullet&&buff("upgrade_metal_skin"))return 0;
        invisibleUntil=0;lastDamage=time;zones.removeIf(z->z.item.id().equals("upgrade_health_stimpak")&&z.center==0&&h.enemyHeroDamage());
        for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);
            if(id.equals("upgrade_berserker")){expirations.put(id,time+value(i,"DamageDuration"));double a=accum.getOrDefault(id,0D)+damage;int n=(int)(a/value(i,"DamageToStack"));accum.put(id,a-n*value(i,"DamageToStack"));stacks.merge(id,n,(x,y)->Math.min((int)value(i,"MaxStacks"),x+y));}
            
            if(id.equals("upgrade_absorbing_armor")&&spirit&&damage>=value(i,"DamageThreshold")&&proc(i,value(i,"AbilityCooldown")))h.reduceCooldown(value(i,"CooldownReductionPerHit"),0);
            if((bullet&&id.equals("upgrade_weapon_shielding")||spirit&&id.equals("upgrade_spirit_bubble"))){double a=damageWindows.computeIfAbsent(id,k->new DamageWindow()).add(time,value(i,"DamageWindow"),damage);if(a>=value(i,"DamageThreshold")&&proc(i,value(i,"AbilityCooldown"))){shield(i.id(),value(i,"CombatBarrier"),value(i,"BarrierDuration"));damageWindows.get(id).clear();}}
            if(id.equals("upgrade_spellbreaker")&&spirit&&damage>=value(i,"DamageThreshold")&&proc(i,value(i,"AbilityCooldown")))damage*=1-value(i,"SpiritDamageReductionProc")/100;
            if(id.equals("upgrade_deflecting_armor")&&bullet)damage*=1-value(i,"DeflectionPercent")/100;
            if(id.equals("upgrade_cheat_death")&&damage>=h.health()&&proc(i,value(i,"AbilityCooldown"))){activateBuff(i,value(i,"DeathImmunityDuration"));damage=Math.min(damage,h.health()-1);}
        }
        if(buff("upgrade_cheat_death"))damage=Math.min(damage*.4,Math.max(0,h.health()-1));
        updateShields();double remaining=damage;for(var shield:shields.values().stream().sorted(Comparator.comparingDouble(s->s.until)).toList()){double absorbed=Math.min(shield.amount,remaining);shield.amount-=absorbed;remaining-=absorbed;if(remaining<=0)break;}updateShields();return Math.max(0,remaining);
    }
    public void onParry(Host h){for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);if(id.equals("upgrade_melee_rebuttal")){h.heal(value(i,"HealOnSuccess"));activateBuff(i,value(i,"BuffDuration"));}}}
    public void onControl(Host h){for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);if(Set.of("upgrade_vex_barrier","upgrade_auto_cleanse").contains(id)&&proc(i,value(i,"AbilityCooldown"))){shield(i.id(),value(i,"VexBarrierCombatBarrier"),value(i,"AbilityDuration"));if(id.equals("upgrade_auto_cleanse")){h.cleanse();h.reduceCooldown(0,value(i,"CooldownReductionOnProc"));}}}}
    public boolean procTarget(ItemCatalog.Item i,int target,double cooldown){String key=i.id()+":"+target;if(procs.getOrDefault(key,0D)>time)return false;procs.put(key,time+cooldown);return true;}
    public boolean piercing(){var i=ItemCatalog.ITEMS.get("upgrade_aprounds");return i!=null&&inventory.owned.contains(i.id())&&random.nextDouble()*100<value(i,"ProcChance");}
    public void targetDamaged(int target,boolean heroDamage){if(heroDamage)zones.removeIf(z->z.item.id().equals("upgrade_health_stimpak")&&z.center==target);}
    public void onHealed(double amount){for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);if(id.equals("upgrade_healbuff")&&amount>=value(i,"MinimumHealAmount")&&proc(i,value(i,"AbilityCooldown")))activateBuff(i,value(i,"BuffDuration"));}}
    public void onEnemyAbility(double distance){for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);if(id.equals("upgrade_restorative_locket")&&distance<=value(i,"Radius"))stacks.merge(id,1,(a,b)->Math.min((int)value(i,"MaxStacks"),a+b));}}
    public void onWeaponDamage(int target,double damage,Host h){context(h);for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);if(id.equals("upgrade_rechargingbullets")){var window=damageWindows.computeIfAbsent(id,k->new DamageWindow());if(window.add(time,value(i,"DamageWindow"),damage)>=value(i,"DamageThreshold")&&proc(i,value(i,"AbilityCooldown"))){h.replenishCharge();window.clear();}}}}
    public void beginParry(Host h){var i=ItemCatalog.ITEMS.get("upgrade_counterspell");if(inventory.owned.contains(i.id())&&proc(i,value(i,"AbilityCooldown")))spellParryUntil=time+value(i,"SpellParryDuration");}
    public void breakInvisibility(){if(invisibleUntil>time){invisibleUntil=0;var i=ItemCatalog.ITEMS.get("upgrade_cloaking_device_active");activateBuff(i,value(i,"AmbushDuration"));}}
}
