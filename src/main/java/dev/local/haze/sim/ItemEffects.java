package dev.local.haze.sim;

import java.util.*;

/** Stateful item rules; all magnitudes come from the pinned owned-game catalogue. */
public final class ItemEffects {
    public interface Host {
        double health(); double maxHealth(); void heal(double n); void drain(double n);
        int target(double range); List<Integer> nearby(double range,int center);
        double targetHealth(int target); void damage(int target,double amount); void debuff(int target,ItemCatalog.Item item,double duration);
        default boolean inCombat(){return true;}default boolean behind(int target){return false;}default int point(double range){return -1;}
        void reload(double percent); void refresh(boolean ultimate); void teleport(double range,int target); void impulse(double speed); void cleanse();
    }
    public final ItemInventory inventory;
    public double time,barrier,barrierUntil,invisibleUntil,flightUntil,flightSpeed,moveImpulse;
    public long movementSerial,staminaSerial;
    public final Map<String,Double> buffs=new HashMap<>();
    public final Map<String,Integer> stacks=new HashMap<>();
    private final Map<String,Double> procs=new HashMap<>(), accum=new HashMap<>();
    private final List<RunnableAt> delayed=new ArrayList<>();
    private final List<Zone> zones=new ArrayList<>();
    private double lastShot=-100,shootStart;
    private final Map<String,Double> expirations=new HashMap<>();
    private final Random random=new Random(25950029);
    private record RunnableAt(double at,Runnable run){}
    private record Zone(ItemCatalog.Item item,int center,double until,double started){}
    public ItemEffects(ItemInventory inventory){this.inventory=inventory;}
    public boolean buff(String id){return inventory.owned.contains(id)&&buffs.getOrDefault(id,0D)>time;}
    public double cooldown(String id){return Math.max(0,inventory.readyAt.getOrDefault(id,0D)-time);}
    public boolean proc(ItemCatalog.Item i,double cooldown){String id=i.id();if(procs.getOrDefault(id,0D)>time)return false;procs.put(id,time+Math.max(0,cooldown));return true;}
    private double duration(ItemCatalog.Item i){for(String k:List.of("AbilityDuration","BuffDuration","CasterBuffDuration","BarrierDuration","BonusShotsDuration","RegenDuration"))if(i.property(k)>0)return i.property(k);return 0;}
    public void activateBuff(ItemCatalog.Item i,double duration){buffs.put(i.id(),time+duration);}
    public void shield(double amount,double duration){barrier=Math.max(barrier,amount);barrierUntil=Math.max(barrierUntil,time+duration);}
    public boolean use(int slot,Host h){
        var active=inventory.active();if(slot<0||slot>=active.size())return false;
        var i=ItemCatalog.ITEMS.get(active.get(slot));String id=i.id();
        if(id.equals("upgrade_blood_tribute")&&buff(id)){buffs.remove(id);return true;}
        if(cooldown(id)>0)return false;
        boolean enemy=Set.of("upgrade_non_player_bonus_sacrifice","upgrade_containment","upgrade_spirit_sap","upgrade_focus_lens","upgrade_withering_whip","upgrade_greater_withering_whip","upgrade_rupture","upgrade_target_stun","upgrade_phantom_strike","upgrade_glitch").contains(id);
        int target=enemy?h.target(i.property("AbilityCastRange")):0;
        if(enemy&&target<0){inventory.message="Нет цели в пределах дальности";return false;}
        inventory.readyAt.put(id,time+i.property("AbilityCooldown")*(1-inventory.reduction("ItemCooldownReduction")));
        inventory.message=i.name();
        double delay=Math.max(0,i.property("AbilityCastDelay"));
        if(delay>0)delayed.add(new RunnableAt(time+delay,()->execute(i,target,h)));else execute(i,target,h);
        return true;
    }
    private void execute(ItemCatalog.Item i,int target,Host h){
        String id=i.id();double d=duration(i);activateBuff(i,d);
        switch(id){
            case "upgrade_non_player_bonus_sacrifice" -> {h.damage(target,h.targetHealth(target)*10);activateBuff(i,d);}
            case "upgrade_capacitor" -> {for(int t:h.nearby(i.property("ChainRadius"),0)){h.damage(t,i.property("Damage"));h.debuff(t,i,i.property("SlowDuration"));}}
            case "upgrade_targeted_silence" -> {for(int t:h.nearby(i.property("AbilityCastRange"),-2)){h.damage(t,i.property("Damage"));h.debuff(t,i,d);}}
            case "upgrade_reduce_debuff_duration" -> {h.cleanse();h.heal(i.property("HealOnActivate"));}
            case "upgrade_grit","upgrade_guardian_ward","upgrade_divine_barrier" -> shield(Math.max(i.property("CombatBarrier"),i.property("GuardianWardCombatBarrier")),d);
            case "upgrade_containment","upgrade_spirit_sap","upgrade_focus_lens","upgrade_withering_whip","upgrade_greater_withering_whip","upgrade_glitch" -> h.debuff(target,i,d);
            case "upgrade_rupture" -> {h.debuff(target,i,d);zones.add(new Zone(i,target,time+d,time));}
            case "upgrade_discord" -> zones.add(new Zone(i,0,time+d,time));
            case "upgrade_target_stun" -> delayed.add(new RunnableAt(time+i.property("StunDelay"),()->h.debuff(target,i,i.property("StunDuration"))));
            case "upgrade_phantom_strike" -> {h.teleport(i.property("AbilityCastRange"),target);h.damage(target,i.property("ImpactDamage"));h.debuff(target,i,i.property("SlowDuration"));}
            case "upgrade_warp_stone" -> h.teleport(i.property("AbilityCastRange"),-1);
            case "upgrade_aoe_root" -> {for(int t:h.nearby(i.property("CaptureRadius"),h.point(i.property("AbilityCastRange"))))h.debuff(t,i,d);}
            case "upgrade_ability_refresher" -> h.refresh(true);
            case "upgrade_ability_power_shard" -> h.refresh(false);
            case "upgrade_cloaking_device_active" -> invisibleUntil=time+d;
            case "upgrade_rocket_booster" -> {moveImpulse=i.property("JumpVelocityHidden");movementSerial++;h.impulse(moveImpulse);shield(i.property("CombatBarrier"),i.property("BarrierDuration"));}
            case "upgrade_health_nova","upgrade_health_stimpak" -> zones.add(new Zone(i,0,time+i.property("RegenDuration"),time));
            case "upgrade_restorative_locket" -> {h.heal(i.property("HealPerStack")*stacks.getOrDefault(id,0));stacks.put(id,0);staminaSerial++;}
            case "upgrade_rescue_beam" -> {double heal=h.maxHealth()*i.property("HealPercentAmount")/100;h.heal(heal);}
            case "upgrade_thermal_detonator" -> zones.add(new Zone(i,h.point(i.property("Radius")),time+d,time));
            case "upgrade_blood_tribute" -> activateBuff(i,Double.MAX_VALUE);
            case "upgrade_unstoppable" -> h.cleanse();
            case "upgrade_cold_front","upgrade_arctic_blast" -> {for(int t:h.nearby(i.property("EndRadius"),0)){h.damage(t,i.property("Damage"));h.debuff(t,i,Math.max(d,i.property("SlowDuration")));}}
            case "upgrade_self_bubble" -> {h.cleanse();h.reload(100);}
            case "upgrade_surging_power" -> h.reload(i.property("ActiveReloadPercent"));
            case "upgrade_magic_carpet" -> {flightUntil=time+i.property("SummonDuration")+d;flightSpeed=i.property("FlyMoveSpeed");}
            case "upgrade_goose_egg" -> inventory.message="Души уже бесконечны";
            default -> { /* Self buffs are applied by bonus(), immunity(), and onBullet(). */ }
        }
    }
    public void tick(double dt,Host h){
        double previous=time;time+=dt;if(time>=barrierUntil)barrier=0;
        var tasks=new ArrayList<>(delayed);delayed.clear();for(var task:tasks)if(task.at<=time)task.run.run();else delayed.add(task);
        buffs.keySet().removeIf(id->!inventory.owned.contains(id)||buffs.get(id)<=time);
        for(var z:new ArrayList<>(zones)){
            if(z.until<=previous||!inventory.owned.contains(z.item.id())){zones.remove(z);continue;}
            var i=z.item;double elapsed=Math.min(time,z.until)-Math.max(previous,z.started);
            if(i.property("TotalHealthRegen")>0){h.heal(i.property("TotalHealthRegen")/i.property("RegenDuration")*elapsed);continue;}
            double radius=Math.max(i.property("Radius"),i.property("AuraRadius"));
            List<Integer> targets=z.center>0&&(i.id().equals("upgrade_rupture")||i.property("DotDuration")>0||i.id().equals("upgrade_weapon_backstabber"))?List.of(z.center):h.nearby(radius,z.center);
            for(int t:targets){double damage=i.property("DPS");
                damage+=h.targetHealth(t)*(i.property("MaxHealthPercentAsDPS")+i.property("DotHealthPercent"))/100;
                if(i.property("DPSIncrease")>0)damage=Math.min(i.property("DPSMax"),damage+(time-z.started)*i.property("DPSIncrease"));
                h.damage(t,damage*elapsed);h.debuff(t,i,.2);
            }
        }
        if(buff("upgrade_blood_tribute")){var i=ItemCatalog.ITEMS.get("upgrade_blood_tribute");if(h.health()<=i.property("HealthDrainedPerSecond")*dt+1)buffs.remove(i.id());else h.drain(i.property("HealthDrainedPerSecond")*dt);}
        for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);
            if(buff(id)&&i.property("Regeneration")>0)h.heal(i.property("Regeneration")*dt);
            if(!h.inCombat()&&i.property("HealLifePercentOutOfCombat")>0)h.heal(h.maxHealth()*i.property("HealLifePercentOutOfCombat")/100*dt);
            if(expirations.getOrDefault(id,Double.MAX_VALUE)<=time){stacks.remove(id);accum.remove(id);expirations.remove(id);}
            if(id.equals("upgrade_tech_damage_pulse")&&proc(i,1.5))for(int t:h.nearby(i.property("DamagePulseRadius"),0))h.damage(t,i.property("DamagePulseAmount"));
            if(id.equals("upgrade_bullet_armor_reduction_aura")||id.equals("upgrade_juggernaut")||buff(id)&&id.equals("upgrade_colossus"))for(int t:h.nearby(i.property("Radius")>0?i.property("Radius"):8,0))h.debuff(t,i,.2);
        }
    }
    public double bonus(String key,double healthPercent){
        double value=0;
        for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);boolean active=buff(id);
            if(active){value+=switch(key){
                case "BonusMoveSpeed" -> (id.equals("upgrade_cloaking_device_active")&&invisibleUntil>time?i.property("InvisMoveSpeedMod"):0)+(id.equals("upgrade_counterspell")||id.equals("upgrade_weapon_backstabber")||id.equals("upgrade_resonant_healing")||id.equals("upgrade_magic_storm")?i.property("BonusMoveSpeed"):0)+i.property("ActiveBonusMoveSpeed")+i.property("ActiveMoveSpeedPenalty")+(id.equals("upgrade_blood_tribute")||id.equals("upgrade_guardian_ward")||id.equals("upgrade_divine_barrier")?i.property("BonusMoveSpeed"):0);
                case "BonusFireRate" -> (id.equals("upgrade_cloaking_device_active")&&invisibleUntil<=time?i.property("AmbushBonusFireRate"):0)+i.property("ActiveBonusFireRate")+(Set.of("upgrade_dps_aura","upgrade_blood_tribute","upgrade_active_reload","upgrade_kinetic_sash","upgrade_tech_overflow","upgrade_healbuff").contains(id)?i.property("BonusFireRate"):0)+i.property("ActivatedFireRate")+i.property("FireRateBonus");
                case "SpiritPower" -> (id.equals("upgrade_spirit_snatch")?i.property("TechPowerGain"):0)+(id.equals("upgrade_cloaking_device_active")&&invisibleUntil<=time?i.property("AmbushBonusTechPower"):0)+i.property("BonusSpirit")+(id.equals("upgrade_arcane_surge")?i.property("SpiritPower"):0);
                case "BulletLifestealPercent" -> i.property("ActiveBonusLifesteal");
                case "AbilityLifestealPercentHero" -> id.equals("upgrade_infuser")?i.property(key):i.property("BonusSpiritLifesteal");
                case "BulletResist" -> i.property("BuffBulletResist")+(id.equals("upgrade_warp_stone")?i.property("BulletResist"):0);
                case "TechResist" -> i.property("BuffTechResist")+(id.equals("upgrade_spirit_snatch")?i.property("TechArmorGain"):0);
                case "BonusMeleeDamagePercent" -> id.equals("upgrade_colossus")?i.property(key):id.equals("upgrade_cloaking_device_active")&&invisibleUntil<=time?i.property("AmbushBonusMeleeDamage"):0;
                case "BonusClipSize" -> id.equals("upgrade_kinetic_sash")?i.property(key):0;
                default -> 0;};}
            if(i.property("LifeThreshold")>0&&healthPercent>=i.property("LifeThreshold")&&!i.stats().containsKey(key))value+=i.property(key);
            if(id.equals("upgrade_chonky")&&healthPercent>=i.property("HealthThreshold")&&key.equals("BonusMoveSpeed"))value+=i.property(key);
            if((id.equals("upgrade_tech_purge")&&key.equals("TechResist")||id.equals("upgrade_improved_bullet_armor")&&key.equals("BulletResist"))&&!i.stats().containsKey(key))value+=i.property(key);
            if(id.equals("upgrade_intensifying_clip")&&key.equals("BaseAttackDamagePercent")&&time-lastShot<.3)value+=i.property("BaseAttackDamagePercentAtMaxDuration")*Math.min(1,(time-shootStart)/i.property("ShootDurationForMax"));
            if(id.equals("upgrade_reinforcing_casings")&&key.equals("BulletResist"))value+=stacks.getOrDefault(id,0)*i.property("BulletResistPerStack");
            if(id.equals("upgrade_trophy_collector"))value+=stacks.getOrDefault(id,0)*i.property("Stacking"+key);
            if(id.equals("upgrade_glass_cannon")&&key.equals("BonusClipSize"))value+=stacks.getOrDefault(id,0)*i.property("BonusClipPerKill");
            if(active&&id.equals("upgrade_headhunter")&&key.equals("BonusMoveSpeed"))value+=i.property(key);
            if(active&&id.equals("upgrade_melee_rebuttal")&&key.equals("BonusMeleeDamagePercent"))value+=i.property("BonusDamagePercent");
            if(i.property("HealthThreshold")>0&&healthPercent<i.property("HealthThreshold"))value+=i.property(key+"BelowThreshold");
            if(id.equals("upgrade_fervor")&&healthPercent<i.property("LowHealthThreshold"))value+=key.equals("BonusFireRate")?i.property("FervorFireRate"):key.equals("BonusMoveSpeed")?i.property("FervorMovespeed"):0;
            if(key.equals("BaseAttackDamagePercent"))value+=stacks.getOrDefault(id,0)*(id.equals("upgrade_bulletshredimbue")?i.property("WeaponPowerPerStackNonHero"):i.property("WeaponPowerPerStack"));
            if(key.equals("BonusFireRate"))value+=stacks.getOrDefault(id,0)*i.property("FireRatePerKill");
        }return value;
    }
    public void onShot(){if(time-lastShot>.3)shootStart=time;lastShot=time;}
    public double onBullet(int target,double damage,boolean head,double distance,double targetHealthPercent,Host h){
        for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);
            if(distance<i.property("CloseRangeBonusDamageRange"))damage*=1+i.property("CloseRangeBonusWeaponPower")/100;
            if(i.property("LongRangeBonusWeaponPowerMinRange")>0&&distance>=i.property("LongRangeBonusWeaponPowerMinRange"))damage*=1+i.property("LongRangeBonusWeaponPower")/100;
            if(i.property("EnemyLifeThreshold")>0&&targetHealthPercent>=i.property("EnemyLifeThreshold"))damage*=1+i.property("BaseAttackDamagePercentBonus")/100;
            if(head&&i.property("HeadShotBonusDamage")>0&&proc(i,i.property("AbilityCooldown"))){damage+=i.property("HeadShotBonusDamage");h.heal(h.maxHealth()*i.property("HealPercentPerHeadshot")/100);activateBuff(i,i.property("MovementSpeedBonusDuration"));}
            if(i.property("CritDamagePercent")>0&&random.nextDouble()*100<i.property("ProcChance"))damage*=1+i.property("CritDamagePercent")/100;
            if(i.property("ProcBonusMagicDamage")>0&&proc(i,i.property("AbilityCooldown")))h.damage(target,i.property("ProcBonusMagicDamage"));
            if(i.property("HealFromNPC")>0&&proc(i,i.property("AbilityCooldown")))h.heal(i.property("HealFromNPC"));
            if(i.property("RicochetTargets")>0){int n=0;for(int t:h.nearby(i.property("RicochetRadius"),target))if(t!=target&&n++<i.property("RicochetTargets"))h.damage(t,damage*i.property("RicochetDamagePercent")/100);}
            if(i.property("DamagePerChain")>0&&random.nextDouble()*100<i.property("ProcChance")&&proc(i,i.property("ProcCooldown"))){int n=0;for(int t:h.nearby(i.property("ChainRadius"),target))if(n++<i.property("ChainCount"))h.damage(t,i.property("DamagePerChain"));}
            boolean buildup=i.property("BuildUpPerShot")>0;
            boolean trigger=!buildup;
            if(buildup){String k=id+":"+target;double a=accum.getOrDefault(k,0D)+i.property("BuildUpPerShot");trigger=a>=i.property("BuildUpDuration");accum.put(k,trigger?0:a);}
            boolean headOnly=id.contains("headshot")||id.equals("upgrade_banshee_slugs");
            if(trigger&&(!headOnly||head)&&!i.active()){
                h.debuff(target,i,Math.max(i.property("DebuffDuration"),Math.max(i.property("SlowDuration"),i.property("AbilityDuration"))));
                if(i.property("DotDuration")>0)zones.add(new Zone(i,target,time+i.property("DotDuration"),time));
                if(id.equals("upgrade_tech_overflow"))activateBuff(i,duration(i));
            }
            if(id.equals("upgrade_burst_fire")&&proc(i,i.property("AbilityCooldown")))activateBuff(i,duration(i));
            if(id.equals("upgrade_reinforcing_casings")){stacks.merge(id,1,(a,b)->Math.min((int)i.property("MaxArmorStacks"),a+b));expirations.put(id,time+i.property("BulletResistDuration"));}
            if(id.equals("upgrade_weapon_backstabber")&&h.behind(target)&&proc(i,i.property("AbilityCooldown"))){zones.add(new Zone(i,target,time+i.property("DebuffDuration"),time));activateBuff(i,i.property("DebuffDuration"));}
            if(buff("upgrade_ethereal_bullets")&&id.equals("upgrade_ethereal_bullets"))h.damage(target,i.property("BulletsBonusMagicDamage"));
            if(id.equals("upgrade_siphon_bullets")&&proc(i,i.property("ProcCooldown")))h.heal(h.targetHealth(target)*i.property("HealthStealPctHero")/100);
            if(id.equals("upgrade_express_shot")&&proc(i,i.property("AbilityCooldown")))damage*=1+i.property("ProcBaseAttackDamagePercent")/100;
            if(buff(id)&&id.equals("upgrade_split_shot")){int n=0;for(int t:h.nearby(15,target))if(t!=target&&n++<i.property("BulletSplitShot"))h.damage(t,damage);}
        }return damage;
    }
    public void onMelee(int target,boolean heavy,Host h){for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);
        if((i.property("SpiritDamage")>0||i.property("LifestrikeHeal")>0||i.property("LifestealHeal")>0)&&proc(i,i.property("AbilityCooldown")*(heavy?1:Math.max(1,i.property("LightMeleeCooldownMult"))))){
            if(id.equals("upgrade_spirit_snatch"))activateBuff(i,i.property("AbilityDuration"));
            h.damage(target,i.property("SpiritDamage"));h.heal((i.property("LifestrikeHeal")+i.property("LifestealHeal")+h.maxHealth()*i.property("LifestealHealPercent")/100)*i.property("NonHeroHealPct")/100);h.debuff(target,i,Math.max(i.property("AbilityDuration"),i.property("SlowDuration")));}
        if(heavy&&i.property("BonusHeavyMeleeDamage")>0&&proc(i,i.property("AbilityCooldown"))){h.damage(target,i.property("BonusHeavyMeleeDamage"));h.reload(100);
            if(id.equals("upgrade_crushing_fists")){String key=id+":"+target;int n=stacks.merge(key,(int)i.property("HeavyMeleeMultiplier"),Integer::sum);if(n>=i.property("MaxStacks")){stacks.put(key,0);h.debuff(target,i,i.property("StunDuration"));}}}
    }}
    public void onCast(int index,Host h){for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);
        if(id.equals("upgrade_restorative_locket"))stacks.merge(id,1,(a,b)->Math.min((int)i.property("MaxStacks"),a+b));
        if(index==3&&id.equals("upgrade_diviners_kevlar")&&proc(i,i.property("AbilityCooldown")))shield(i.property("CombatBarrier"),i.property("BuffDuration"));
        if(Set.of("upgrade_resonant_healing","upgrade_mystic_regeneration","upgrade_quick_silver","upgrade_ethereal_bullets","upgrade_magic_storm","upgrade_enchanted_holsters").contains(id)&&proc(i,i.property("AbilityCooldown"))){h.heal(i.property("HealingPerCast"));h.reload(i.property("AmmoReloadPercent"));activateBuff(i,Math.max(duration(i),i.property("RegenerationDuration")));}
    }}
    public void onSpirit(int target,double damage,Host h){for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);
        if(Set.of("upgrade_magic_slow","upgrade_suppressor","upgrade_healbane","upgrade_bullet_resist_shredder","upgrade_escalating_exposure").contains(id))h.debuff(target,i,Math.max(i.property("AbilityDuration"),i.property("DebuffDuration")));
        if(Set.of("upgrade_magic_vulnerability","upgrade_spirit_burn").contains(id))h.debuff(target,i,Math.max(8,i.property("DebuffDuration")));
        if(Set.of("upgrade_magic_burst","upgrade_magic_shock").contains(id)&&damage>=i.property("MinimumDamage")&&proc(i,Math.max(i.property("AbilityCooldown"),i.property("ReProcLockoutTime"))))h.damage(target,i.property("Damage")+h.targetHealth(target)*i.property("CurrentHealthDamage")/100);
        if(id.equals("upgrade_spirit_burn")){String key=id+":"+target;double a=accum.merge(key,damage,Double::sum);if(a>=i.property("DamageThreshold")&&proc(i,i.property("ImmunityDuration"))){accum.put(key,0D);h.damage(target,i.property("ExplosionDamage"));zones.add(new Zone(i,target,time+i.property("DebuffDuration"),time));}}
        if(id.equals("upgrade_bulletshredimbue")){stacks.merge(id,1,(a,b)->Math.min((int)i.property("NonHeroStackLimit"),a+b));expirations.put(id,time+i.property("AbilityDuration"));}
        if(i.property("ProcBonusDamage")>0&&proc(i,i.property("AbilityCooldown")))h.damage(target,i.property("ProcBonusDamage"));
        if(id.equals("upgrade_mystic_reverb")&&proc(i,i.property("AbilityCooldown")))delayed.add(new RunnableAt(time+i.property("DelayDuration"),()->{for(int t:h.nearby(i.property("Radius"),target))h.damage(t,damage*i.property("TechDamagePercent")/100);}));
    }}
    public double incoming(double damage,boolean bullet,boolean spirit,Host h){
        invisibleUntil=0;if(buff("upgrade_self_bubble")||bullet&&buff("upgrade_metal_skin"))return 0;
        for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);
            if(id.equals("upgrade_berserker")){expirations.put(id,time+i.property("DamageDuration"));double a=accum.getOrDefault(id,0D)+damage;int n=(int)(a/i.property("DamageToStack"));accum.put(id,a-n*i.property("DamageToStack"));stacks.merge(id,n,(x,y)->Math.min((int)i.property("MaxStacks"),x+y));}
            if(id.equals("upgrade_rechargingbullets")){double a=accum.merge(id,damage,Double::sum);if(a>=i.property("DamageThreshold")&&proc(i,i.property("AbilityCooldown"))){h.reload(100);accum.put(id,0D);}}
            if(id.equals("upgrade_absorbing_armor")&&spirit&&damage>=i.property("DamageThreshold")&&proc(i,i.property("AbilityCooldown")))h.refresh(false);
            if((bullet&&id.equals("upgrade_weapon_shielding")||spirit&&id.equals("upgrade_spirit_bubble"))){double a=accum.getOrDefault(id,0D)+damage;accum.put(id,a);if(a>=i.property("DamageThreshold")&&proc(i,i.property("AbilityCooldown"))){shield(i.property("CombatBarrier"),i.property("BarrierDuration"));accum.put(id,0D);}}
            if(id.equals("upgrade_spellbreaker")&&spirit&&damage>=i.property("DamageThreshold")&&proc(i,i.property("AbilityCooldown")))damage*=1-i.property("SpiritDamageReductionProc")/100;
            if(id.equals("upgrade_deflecting_armor")&&bullet)damage*=1-i.property("DeflectionPercent")/100;
            if(id.equals("upgrade_cheat_death")&&damage>=h.health()&&proc(i,i.property("AbilityCooldown"))){activateBuff(i,i.property("DeathImmunityDuration"));damage=Math.min(damage,h.health()-1);}
        }
        if(buff("upgrade_cheat_death"))damage=Math.min(damage*.4,Math.max(0,h.health()-1));
        double absorbed=Math.min(barrier,damage);barrier-=absorbed;return Math.max(0,damage-absorbed);
    }
    public void onParry(Host h){for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);if(id.equals("upgrade_melee_rebuttal")||id.equals("upgrade_counterspell")){h.heal(i.property("HealOnSuccess"));activateBuff(i,i.property("BuffDuration"));}}}
    public void onControl(Host h){for(String id:inventory.owned){var i=ItemCatalog.ITEMS.get(id);if(Set.of("upgrade_vex_barrier","upgrade_auto_cleanse").contains(id)&&proc(i,i.property("AbilityCooldown"))){shield(i.property("VexBarrierCombatBarrier"),i.property("AbilityDuration"));if(id.equals("upgrade_auto_cleanse"))h.cleanse();}}}
    public void breakInvisibility(){if(invisibleUntil>time){invisibleUntil=0;var i=ItemCatalog.ITEMS.get("upgrade_cloaking_device_active");activateBuff(i,i.property("AmbushDuration"));}}
}
