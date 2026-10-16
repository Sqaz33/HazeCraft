package dev.local.haze.sim.movement;

import dev.local.haze.sim.V3;

/** Current native mantle exit: cap existing speed, then add the crouch-input boost. */
public final class MantleExit {
    public static V3 velocity(V3 previous,V3 input,V3 mantleDirection,double maxSpeed,double boost,boolean crouch,boolean grounded){
        V3 result=previous;
        if(result.horizontal()>maxSpeed)result=result.normalized().mul(maxSpeed);
        V3 wish=input.flat().normalized();
        if(grounded&&crouch&&wish.dot(mantleDirection.flat().normalized())>0)
            result=result.add(wish.mul(boost));
        return result;
    }
}
