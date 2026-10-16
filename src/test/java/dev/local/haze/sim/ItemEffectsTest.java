package dev.local.haze.sim;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ItemEffectsTest {
    static final class World implements ItemEffects.Host {
        double hp=300,healed,damage,reloaded,warp,impulse;int refresh,cleanse,statuses;boolean hasTarget=true;
        public double health(){return hp;}public double maxHealth(){return 730;}
        public void heal(double n){healed+=n;hp=Math.min(730,hp+n);}public void drain(double n){hp-=n;}
        public int target(double range){return hasTarget?1:-1;}public List<Integer> nearby(double r,int c){return List.of(1,2);}
        public double targetHealth(int t){return 1000;}public void damage(int t,double n){damage+=n;}public void debuff(int t,ItemCatalog.Item i,double d){statuses++;}
        public void reload(double n){reloaded+=n;}public void refresh(boolean all){refresh+=all?2:1;}
        public void teleport(double n,int t){warp=n;}public void impulse(double n){impulse=n;}public void cleanse(){cleanse++;}
    }
    static ItemEffects item(String id){var inventory=new ItemInventory();assertTrue(inventory.buy(id));return new ItemEffects(inventory);}
    @Test void activeCooldownPreventsRepeatedDamageAndWorksAgainAfterSourceCooldown(){
        var e=item("upgrade_cold_front");var h=new World();assertTrue(e.use(0,h));assertEquals(190,h.damage);assertFalse(e.use(0,h));assertEquals(190,h.damage);
        e.tick(26,h);assertTrue(e.use(0,h));assertEquals(380,h.damage);
    }
    @Test void targetFailureDoesNotSpendCooldown(){var e=item("upgrade_phantom_strike");var h=new World();h.hasTarget=false;assertFalse(e.use(0,h));assertEquals(0,e.cooldown("upgrade_phantom_strike"));h.hasTarget=true;assertTrue(e.use(0,h));e.tick(.4,h);assertEquals(25,h.warp);assertEquals(75,h.damage);}
    @Test void barriersAbsorbExactlyOnceAndExpire(){var e=item("upgrade_grit");var h=new World();e.use(0,h);assertEquals(200,e.barrier);assertEquals(0,e.incoming(120,true,false,h));assertEquals(80,e.barrier);assertEquals(70,e.incoming(150,true,false,h));assertEquals(0,e.barrier);e.tick(61,h);e.use(0,h);e.tick(5,h);assertEquals(0,e.barrier);}
    @Test void metalSkinImmuneOnlyDuringDuration(){var e=item("upgrade_metal_skin");var h=new World();e.use(0,h);assertEquals(0,e.incoming(50,true,false,h));assertEquals(50,e.incoming(50,false,true,h));e.tick(5.1,h);assertEquals(50,e.incoming(50,true,false,h));}
    @Test void tributeTogglesDrainsAndRemovesBuffOnSale(){var e=item("upgrade_blood_tribute");var h=new World();e.use(0,h);assertEquals(35,e.bonus("BonusFireRate",100));e.tick(.1,h);assertEquals(295,h.hp);e.use(0,h);assertEquals(0,e.bonus("BonusFireRate",100));e.use(0,h);e.inventory.sell("upgrade_blood_tribute");assertEquals(0,e.bonus("BonusFireRate",100));}
    @Test void healingIsSpreadAcrossOriginalDuration(){var e=item("upgrade_health_nova");var h=new World();e.use(0,h);e.tick(.25,h);assertEquals(0,h.healed);for(int i=0;i<35;i++)e.tick(.05,h);assertTrue(h.healed<=325);assertTrue(h.healed>250);}
    @Test void headshotBonusUsesItsOwnCooldownAndRangeDamageIsConditional(){var e=item("upgrade_headshot_booster");var h=new World();assertEquals(55,e.onBullet(1,10,true,5,100,h));assertEquals(10,e.onBullet(1,10,true,5,100,h));var close=item("upgrade_close_range");assertEquals(12,close.onBullet(1,10,false,5,100,h),1e-6);assertEquals(10,close.onBullet(1,10,false,20,100,h));}
    @Test void meleeHasHealingDamageAndProcCooldown(){var e=item("upgrade_lifestrike_gauntlets");var h=new World();e.onMelee(1,true,h);assertEquals(30,h.healed);e.onMelee(1,true,h);assertEquals(30,h.healed);e.tick(9,h);e.onMelee(1,true,h);assertEquals(60,h.healed);}
    @Test void allEnabledActivesAcceptInputAndProduceFiniteCooldowns(){for(var i:ItemCatalog.ITEMS.values())if(i.active()){var e=item(i.id());var h=new World();assertTrue(e.use(0,h),i.id());e.tick(1,h);assertTrue(Double.isFinite(e.cooldown(i.id())),i.id());assertTrue(Double.isFinite(h.damage),i.id());}}
    @Test void sellingBuffedItemStopsItsEffect(){var e=item("upgrade_fleetfoot_boots");var h=new World();e.use(0,h);assertEquals(3,e.bonus("BonusMoveSpeed",100));e.inventory.sell("upgrade_fleetfoot_boots");assertEquals(0,e.bonus("BonusMoveSpeed",100));}
}
