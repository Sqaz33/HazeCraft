package dev.local.haze.sim.movement;

import dev.local.haze.sim.V3;

/** Support integration in a single common unit system. Y is up; no host dependencies. */
public final class SupportSpring {
    private SupportSpring() {}
    public record Result(V3 springVelocity, V3 velocity) {}

    public static Result integrate(V3 springVelocity, V3 velocity, V3 support,
            float originUp, boolean grounded, int movementMode, boolean disabled,
            float frequency, float damping, float fastSpeedSquared, float dt) {
        float vx=(float)velocity.x(),vy=(float)velocity.y(),vz=(float)velocity.z();
        boolean hasSupport=(float)support.x()!=0 || (float)support.y()!=0 || (float)support.z()!=0;
        if ((!grounded && (movementMode!=2 || vy>=0)) || !hasSupport || disabled)
            return new Result(V3.ZERO,velocity);
        float speedSquared=vx*vx+vz*vz+vy*vy;
        float omega=frequency*6.2831855f;
        float effectiveDamping=speedSquared>fastSpeedSquared?.25f:damping;
        float drag=effectiveDamping+effectiveDamping;
        drag=drag*omega;
        drag=drag*dt+1;
        float step=omega*dt;
        float correction=omega*omega;
        correction=correction*dt;
        correction=correction*(originUp-(float)support.y());
        float next=((float)springVelocity.y()-correction)/(drag+step*step);
        return new Result(springVelocity.withY(next),velocity.withY(0));
    }
}
