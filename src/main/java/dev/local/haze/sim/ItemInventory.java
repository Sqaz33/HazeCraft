package dev.local.haze.sim;

import java.util.*;

/** Purchases are authoritative; souls are unlimited, ownership and components are still enforced. */
public final class ItemInventory {
    public final List<String> owned=new ArrayList<>();
    public final Map<String,Double> readyAt=new HashMap<>();
    public int capacity=8;
    public String message="";
    private Map<String,Double> stats=Map.of();
    public Set<String> components(String id){var result=new HashSet<String>();collect(id,result);return result;}
    private void collect(String id,Set<String> result){var item=ItemCatalog.ITEMS.get(id);if(item!=null)for(String c:item.components())if(result.add(c))collect(c,result);}
    public int price(String id){var item=ItemCatalog.ITEMS.get(id);if(item==null)return 0;var consumed=components(id);return Math.max(0,item.price()-owned.stream().filter(consumed::contains).map(ItemCatalog.ITEMS::get).mapToInt(ItemCatalog.Item::price).sum());}
    public boolean buy(String id){
        var item=ItemCatalog.ITEMS.get(id);if(item==null){message="Неизвестный предмет";return false;}
        if(owned.contains(id)||owned.stream().anyMatch(o->components(o).contains(id))){message="Этот предмет или его улучшение уже куплено";return false;}
        var consumed=components(id);
        if(owned.size()-owned.stream().filter(consumed::contains).count()>=capacity){message="Нет свободного слота";return false;}
        if(item.active()&&active().size()-owned.stream().filter(consumed::contains).map(ItemCatalog.ITEMS::get).filter(ItemCatalog.Item::active).count()>=4){message="Доступны четыре активных слота";return false;}
        owned.removeIf(consumed::contains);owned.add(id);rebuild();message="Куплено: "+item.name();return true;
    }
    public boolean sell(String id){if(!owned.remove(id))return false;readyAt.remove(id);rebuild();message="Продано: "+ItemCatalog.ITEMS.get(id).name();return true;}
    public List<String> active(){return owned.stream().filter(id->ItemCatalog.ITEMS.get(id).active()).toList();}
    private void rebuild(){Map<String,Double> next=new HashMap<>();for(String id:owned)ItemCatalog.ITEMS.get(id).stats().forEach((k,v)->next.merge(k,v,Double::sum));stats=Map.copyOf(next);}
    public double stat(String key){return stats.getOrDefault(key,0D);}
    public double reduction(String key){double factor=1;for(String id:owned)factor*=1-Math.clamp(ItemCatalog.ITEMS.get(id).stats().getOrDefault(key,0D)/100,0,.95);return 1-factor;}
}
