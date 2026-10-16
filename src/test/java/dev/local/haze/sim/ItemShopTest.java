package dev.local.haze.sim;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class ItemShopTest {
    @Test void catalogueHasEveryEnabledSourceItemPricesIconsAndOriginalNames(){
        assertEquals(156,ItemCatalog.ITEMS.size());assertEquals(44,ItemCatalog.ITEMS.values().stream().filter(ItemCatalog.Item::active).count());
        for(var item:ItemCatalog.ITEMS.values()){
            assertEquals(800<<(item.tier()-1),item.price());assertFalse(item.name().startsWith("upgrade_"),item.id());
            assertNotNull(ItemCatalog.class.getResource("/assets/haze/retail/"+item.icon()),item.id());
            for(String component:item.components())assertTrue(ItemCatalog.ITEMS.containsKey(component),component);
        }
    }
    @Test void extendedMagazineChangesActualGunDamageCapacityAndSellingRemovesStats(){
        var inventory=new ItemInventory();assertTrue(inventory.buy("upgrade_clip_size"));assertFalse(inventory.buy("upgrade_clip_size"));
        Gun gun=new Gun(Retail.DATA);int initial=gun.clipSize(0);
        gun.itemClipPercent=inventory.stat("BonusClipSizePercent");gun.itemWeaponPercent=inventory.stat("BaseAttackDamagePercent");
        assertEquals((int)Math.floor(initial*1.3),gun.clipSize(0));
        var shots=gun.advance(.1,true,false,true,false,0,0,0);assertEquals(gun.f("m_flBulletDamage")*1.08,shots.getFirst().damage(),1e-8);
        assertTrue(inventory.sell("upgrade_clip_size"));assertEquals(0,inventory.stat("BonusClipSizePercent"));assertFalse(inventory.sell("upgrade_clip_size"));
    }
    @Test void upgradeConsumesComponentAndCreditsItsPriceWithoutDoubleStacking(){
        var inventory=new ItemInventory();assertTrue(inventory.buy("upgrade_health"));double original=inventory.stat("BonusHealth");assertEquals(210,original);
        assertEquals(ItemCatalog.ITEMS.get("upgrade_chonky").price()-800,inventory.price("upgrade_chonky"));
        assertTrue(inventory.buy("upgrade_chonky"));assertFalse(inventory.owned.contains("upgrade_health"));assertEquals(1,inventory.owned.size());
        assertEquals(ItemCatalog.ITEMS.get("upgrade_chonky").stats().get("BonusHealth"),inventory.stat("BonusHealth"));assertFalse(inventory.buy("upgrade_health"));
    }
    @Test void slotLimitUnknownIdsAndFailedBuysLeaveInventoryUnchanged(){
        var inventory=new ItemInventory();inventory.capacity=1;assertTrue(inventory.buy("upgrade_health"));
        assertFalse(inventory.buy("upgrade_clip_size"));assertFalse(inventory.buy("fake"));assertEquals(java.util.List.of("upgrade_health"),inventory.owned);
        assertTrue(inventory.buy("upgrade_chonky"));assertEquals(1,inventory.owned.size());
    }
    @Test void itemSpiritAndCooldownDurationApplyToRealAbilityProperties(){
        var a=new Abilities(Retail.DATA);double cooldown=a.p(0,"AbilityCooldown"),sleep=a.p(0,"SleepDuration");
        a.itemCooldownReduction=.2;a.itemDurationPercent=25;
        assertEquals(cooldown*.8,a.p(0,"AbilityCooldown"),1e-8);assertEquals(sleep*1.25,a.p(0,"SleepDuration"),1e-8);
        assertTrue(a.cast(0));assertEquals(cooldown*.8,a.cooldown(0),1e-8);
    }
}
