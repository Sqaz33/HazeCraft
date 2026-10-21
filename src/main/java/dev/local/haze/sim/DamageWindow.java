package dev.local.haze.sim;
import java.util.ArrayDeque;
/** Damage thresholds must not retain hits outside the source property's window. */
public final class DamageWindow {
    private record Hit(double time,double amount){}
    private final ArrayDeque<Hit> hits=new ArrayDeque<>();
    public double add(double time,double window,double amount){
        while(!hits.isEmpty()&&hits.getFirst().time<time-window)hits.removeFirst();
        if(amount>0)hits.addLast(new Hit(time,amount));return hits.stream().mapToDouble(Hit::amount).sum();
    }
    public void clear(){hits.clear();}
}
