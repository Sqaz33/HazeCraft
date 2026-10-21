package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SourceItemRulesTest {
    static class Host implements ItemEffects.Host {
        double hp=300,damage,spirit,healed,allyHeal,reducedSeconds,reducedPercent;int charges,echo=-1,ability=-1,targetsHit;
        UnitRole role=UnitRole.ENEMY_HERO;
        public double health(){return hp;}public double maxHealth(){return 730;}
        public void heal(double n){hp=Math.min(730,hp+n);healed+=n;}public void drain(double n){hp-=n;}
        public int target(double range){return 1;}public int ally(double range){return 2;}
        public List<Integer> nearby(double range,int center){return List.of(1,3,4);}
        public List<Integer> allies(double range,int center){return List.of(0,2);}
        public UnitRole role(int id){return id==0?UnitRole.SELF:id==2?UnitRole.ALLY_HERO:role;}
        public double targetHealth(int target){return target==0?730:1000;}
        public void damage(int t,double n){damage+=n;targetsHit++;}
        public void debuff(int target,ItemCatalog.Item item,double duration){}
        public void healTarget(int target,double n){if(target==0)heal(n);else allyHeal+=n;}
        public void reload(double pct){}public void refresh(boolean ultimate){}
        public void echo(int ability){echo=ability;}public void replenishCharge(){charges++;}
        public void reduceCooldown(double seconds,double percent){reducedSeconds+=seconds;reducedPercent+=percent;}
        public void teleport(double range,int target){}public void impulse(double speed){}public void cleanse(){}
        public double meleeDamage(){return 100;}
        public double spirit(){return spirit;}public int ability(){return ability;}
    }
    static ItemEffects effect(String id){var inv=new ItemInventory();assertTrue(inv.buy(id));return new ItemEffects(inv);}
    static void advance(ItemEffects effects,Host host,double seconds){int count=(int)Math.ceil(seconds/.01);for(int i=0;i<count;i++)effects.tick(seconds/count,host);}
    @Test void healingNovaHonorsCastDelayAndHealsSelfAndVillagerForFullSourceTotal(){var e=effect("upgrade_health_nova");var h=new Host();e.use(0,h);e.tick(.1,h);assertEquals(0,h.healed);advance(e,h,2.3);assertEquals(325,h.healed,1e-7);assertEquals(325,h.allyHeal,1e-7);}
    @Test void cultistSacrificeRejectsEnemyHeroesAndAcceptsCreepsWithoutWastingCooldown(){var e=effect("upgrade_non_player_bonus_sacrifice");var h=new Host();assertFalse(e.use(0,h));assertEquals(0,e.cooldown("upgrade_non_player_bonus_sacrifice"));h.role=UnitRole.CREEP;assertTrue(e.use(0,h));assertTrue(h.damage>0);}
    @Test void tormentPulseUsesSourceTwoTargetLimitCooldownAndSpiritScaling(){var e=effect("upgrade_tech_damage_pulse");var h=new Host();h.spirit=100;e.tick(.01,h);assertEquals(2,h.targetsHit);assertEquals(2*(25+.23*100),h.damage,1e-7);advance(e,h,1.3);assertEquals(2,h.targetsHit);advance(e,h,.2);assertEquals(4,h.targetsHit);}
    @Test void rechargingRushTracksOutgoingWeaponDamageInWindowRatherThanIncomingDamage(){var e=effect("upgrade_rechargingbullets");var h=new Host();e.incoming(200,true,false,h);assertEquals(0,h.charges);e.onWeaponDamage(1,100,h);advance(e,h,4);e.onWeaponDamage(1,100,h);assertEquals(0,h.charges);e.onWeaponDamage(1,100,h);assertEquals(1,h.charges);}
    @Test void echoShardRefreshesOnlyBoundAbilityAndCannotBindUltimateOrPassive(){var e=effect("upgrade_ability_power_shard");var h=new Host();assertFalse(e.inventory.bind("upgrade_ability_power_shard",3));assertFalse(e.inventory.bind("upgrade_ability_power_shard",2));assertTrue(e.inventory.bind("upgrade_ability_power_shard",1));e.use(0,h);assertEquals(1,h.echo);}
    @Test void witchmailSubtractsSecondsInsteadOfRefillingEveryCooldown(){var e=effect("upgrade_absorbing_armor");var h=new Host();e.incoming(100,false,true,h);assertEquals(4,h.reducedSeconds);assertEquals(0,h.reducedPercent);}
    @Test void locketStacksOnlyFromEnemyAbilitiesWithinOriginalRadius(){var e=effect("upgrade_restorative_locket");var h=new Host();e.onCast(0,h);assertEquals(0,e.stacks.getOrDefault("upgrade_restorative_locket",0));e.onEnemyAbility(33);assertEquals(0,e.stacks.getOrDefault("upgrade_restorative_locket",0));for(int i=0;i<30;i++)e.onEnemyAbility(20);assertEquals(25,e.stacks.get("upgrade_restorative_locket"));e.use(0,h);advance(e,h,.2);assertEquals(400,h.healed);assertEquals(0,h.allyHeal);}
    @Test void independentBarriersStackAndExpireIndependently(){var e=effect("upgrade_grit");assertTrue(e.inventory.buy("upgrade_diviners_kevlar"));var h=new Host();e.use(0,h);e.shield("upgrade_diviners_kevlar",1000,20);assertEquals(1200,e.barrier);assertEquals(0,e.incoming(250,true,false,h));assertEquals(950,e.barrier);advance(e,h,4.5);assertEquals(950,e.barrier);advance(e,h,16);assertEquals(0,e.barrier);}
    @Test void counterspellParryStopsSpellDamageButNotBulletsAndExpires(){var e=effect("upgrade_counterspell");var h=new Host();e.beginParry(h);assertEquals(0,e.incoming(100,false,true,h));assertEquals(150,h.healed);assertEquals(100,e.incoming(100,true,false,h));advance(e,h,1);assertEquals(100,e.incoming(100,false,true,h));}
    @Test void itemScalesUseOriginalScaleFunctionRatherThanFlatDamage(){var i=ItemCatalog.ITEMS.get("upgrade_tech_damage_pulse");assertEquals(25,i.scaled("DamagePulseAmount",0,0));assertEquals(48,i.scaled("DamagePulseAmount",100,0));}
    @Test void boundQuicksilverIgnoresOtherAbilityCasts(){var e=effect("upgrade_quick_silver");var h=new Host();e.inventory.bind("upgrade_quick_silver",1);e.onCast(0,h);assertFalse(e.buff("upgrade_quick_silver"));e.onCast(1,h);assertTrue(e.buff("upgrade_quick_silver"));}
    @Test void friendlyTargetsCannotReceiveOffensiveItemCasts(){var e=effect("upgrade_phantom_strike");var h=new Host();h.role=UnitRole.ALLY_HERO;assertFalse(e.use(0,h));assertEquals(0,h.damage);}
    @Test void lifestrikeHealsFromActualMeleeDamageRatherThanMaximumHealth(){var e=effect("upgrade_boxing_glove");var h=new Host();h.role=UnitRole.CREEP;e.onMelee(1,true,h);assertEquals((120+100*.35)*.4,h.healed,1e-8);}
    @Test void enchantedHolstersNegativeReloadModifierShortensReload(){var gun=new Gun(Retail.DATA);double initial=gun.reloadDuration();gun.itemReloadPercent=-30;assertEquals(initial*.7,gun.reloadDuration(),1e-8);}
    @Test void extraChargeAllowsCastingSecondSmokeBeforeFullCooldown(){var a=new Abilities(Retail.DATA);a.itemCharges=1;a.smokeCharges=2;assertTrue(a.cast(1));a.advance(Math.max(0,a.p(1,"AbilityCooldownBetweenCharge"))+.01);assertTrue(a.cast(1));assertEquals(0,a.smokeCharges);}
    @Test void shadowWeaveUsesOriginalFadeAndAmbushDurationAfterBreaking(){var e=effect("upgrade_cloaking_device_active");var h=new Host();e.use(0,h);assertFalse(e.invisible());advance(e,h,.7);assertTrue(e.invisible());e.breakInvisibility();assertFalse(e.invisible());assertEquals(25,e.bonus("BonusFireRate",100));advance(e,h,5.1);assertEquals(0,e.bonus("BonusFireRate",100));}
    @Test void veilWalkerRespondsToOriginalWorldEventAndRetainsMovementAfterStealthBreak(){var e=effect("upgrade_veil_walker");var h=new Host();e.onVeilCrossed(h);assertEquals(85,h.healed);advance(e,h,.3);assertTrue(e.invisible());e.breakInvisibility();assertEquals(3.5,e.bonus("BonusMoveSpeed",100));}
    @Test void platedArmorDeflectsWholeHitsRatherThanAlwaysReducingDamage(){var e=effect("upgrade_deflecting_armor");var h=new Host();int deflected=0;for(int n=0;n<2000;n++){double damage=e.incoming(10,true,false,h);assertTrue(damage==0||damage==10);if(damage==0)deflected++;}assertTrue(deflected>400&&deflected<800);}
}
