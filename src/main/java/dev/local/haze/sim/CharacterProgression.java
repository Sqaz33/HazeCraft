package dev.local.haze.sim;

/** Only standard upgrades awarded by the original hero level table are accumulated. */
public final class CharacterProgression {
    public int level=1;
    public int maximum(){return Retail.DATA.hero.getAsJsonObject("m_mapLevelInfo").keySet().stream().mapToInt(Integer::parseInt).max().orElse(1);}
    public void set(int value){level=Math.clamp(value,1,maximum());}
    public double bonus(String modifier){
        var hero=Retail.DATA.hero;var upgrades=hero.getAsJsonObject("m_mapStandardLevelUpUpgrades");
        if(!upgrades.has(modifier))return 0;
        int count=0;var levels=hero.getAsJsonObject("m_mapLevelInfo");
        for(int i=2;i<=level;i++){var entry=levels.getAsJsonObject(""+i);if(entry!=null&&entry.has("m_bUseStandardUpgrade")&&entry.get("m_bUseStandardUpgrade").getAsBoolean())count++;}
        return count*Retail.number(upgrades.get(modifier));
    }
}
