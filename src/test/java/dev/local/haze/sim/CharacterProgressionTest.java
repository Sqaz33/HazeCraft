package dev.local.haze.sim;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class CharacterProgressionTest {
    @Test void startingLevelHasNoGrowth(){var p=new CharacterProgression();assertEquals(0,p.bonus("MODIFIER_VALUE_BASE_HEALTH_FROM_LEVEL"));}
    @Test void levelUsesOriginalStandardUpgradeCount(){var p=new CharacterProgression();p.set(10);assertEquals(9*33,p.bonus("MODIFIER_VALUE_BASE_HEALTH_FROM_LEVEL"));assertEquals(9*.5,p.bonus("MODIFIER_VALUE_TECH_POWER"));assertEquals(9*.143,p.bonus("MODIFIER_VALUE_BASE_BULLET_DAMAGE_FROM_LEVEL"),1e-9);}
    @Test void clampsToHeroTableRatherThanMinecraftExperienceLevel(){var p=new CharacterProgression();p.set(999);assertEquals(36,p.level);p.set(-1);assertEquals(1,p.level);}
    @Test void ownerRoleMappingDoesNotTreatVillagersAsCreepsOrHostiles(){assertEquals(UnitRole.SELF,UnitRole.classify(true,false,false,false));assertEquals(UnitRole.ALLY_HERO,UnitRole.classify(false,true,false,true));assertEquals(UnitRole.ENEMY_HERO,UnitRole.classify(false,false,true,true));assertEquals(UnitRole.CREEP,UnitRole.classify(false,false,false,true));assertFalse(UnitRole.ALLY_HERO.damageable());assertTrue(UnitRole.CREEP.damageable());assertFalse(UnitRole.CREEP.hero());assertTrue(UnitRole.ENEMY_HERO.hero());}
}
