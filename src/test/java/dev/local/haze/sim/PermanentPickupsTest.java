package dev.local.haze.sim;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class PermanentPickupsTest {
    @Test void sourceTablesKeepAllPickupStatsAndSourceUnits(){
        assertEquals(2,PermanentPickups.stats("spirit_permanent_pickup").get("TechPower"));
        assertEquals(15,PermanentPickups.stats("hp_permanent_pickup").get("BonusHealth"));
        assertEquals(3,PermanentPickups.stats("wp_permanent_pickup").get("BaseAttackDamagePercent"));
        assertEquals(3,PermanentPickups.stats("ammo_permanent_pickup").get("BonusClipSizePercent"));
        assertEquals(.0999998,PermanentPickups.stats("movespeed_permanent_pickup").get("BonusMoveSpeed"),1e-7);
        assertEquals(2,PermanentPickups.stats("range_permanent_pickup").size());
    }
    @Test void deckUsesNativeRoundedWeightsAndTimeTiers(){
        var deck=new PermanentPickups();var rng=new Random(42);Map<String,Integer> counts=new HashMap<>();
        for(int n=0;n<31;n++)counts.merge(deck.draw(0,rng),1,Integer::sum);
        assertEquals(10,counts.size());assertEquals(6,counts.get("hp_permanent_pickup"));
        assertEquals(2,counts.get("range_permanent_pickup"));assertEquals(3,counts.get("spirit_permanent_pickup"));
        assertTrue(deck.draw(600,rng).endsWith("_lv2"));assertTrue(deck.draw(1800,rng).endsWith("_lv3"));
    }
    @Test void permanentBonusesSurviveSellingItemsAndCombineWithResistReduction(){
        var inv=new ItemInventory();inv.grantPickup("hp_permanent_pickup");inv.grantPickup("bulletresist_permanent_pickup");
        inv.buy("upgrade_bullet_armor");double resistance=inv.reduction("BulletResist");assertTrue(resistance>.005);
        inv.sell("upgrade_bullet_armor");assertEquals(15,inv.stat("BonusHealth"));assertEquals(.005,inv.reduction("BulletResist"),1e-8);
    }
    @Test void gooseChannelsOnceConsumesItemAndCannotFarmInitialSoulsForBuffs(){
        var e=SourceItemRulesTest.effect("upgrade_goose_egg");var h=new SourceItemRulesTest.Host();e.tick(.01,h);
        assertTrue(e.use(0,h));assertFalse(e.use(0,h));e.tick(1.99,h);assertTrue(e.inventory.owned.contains("upgrade_goose_egg"));
        e.tick(.02,h);assertFalse(e.inventory.owned.contains("upgrade_goose_egg"));assertTrue(e.inventory.permanentPickups.isEmpty());assertFalse(e.busy());
    }
    @Test void gooseGivesOnePermanentRewardForEightyAccruedSoulsAndRetainsIt(){
        var e=SourceItemRulesTest.effect("upgrade_goose_egg");var h=new SourceItemRulesTest.Host();
        SourceItemRulesTest.advance(e,h,60);assertEquals(480,e.inventory.gooseSouls);e.use(0,h);SourceItemRulesTest.advance(e,h,2.1);
        assertEquals(1,e.inventory.permanentPickups.values().stream().mapToInt(Integer::intValue).sum());
        assertFalse(e.inventory.owned.contains("upgrade_goose_egg"));e.stop();assertEquals(1,e.inventory.permanentPickups.size());
    }
    @Test void cancellingGooseChannelDoesNotConsumeItemOrGrantBonuses(){
        var e=SourceItemRulesTest.effect("upgrade_goose_egg");var h=new SourceItemRulesTest.Host();e.tick(60,h);e.use(0,h);e.tick(1,h);e.stop();e.tick(2,h);
        assertTrue(e.inventory.owned.contains("upgrade_goose_egg"));assertTrue(e.inventory.permanentPickups.isEmpty());
    }
}
