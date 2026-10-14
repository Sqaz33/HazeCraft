package dev.local.haze.mixin;

import dev.local.haze.*;
import dev.local.haze.sim.Fixation;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Mob.class)
public abstract class MobMixin {
    @Inject(method="serverAiStep",at=@At("HEAD"),cancellable=true)
    private void haze$sleep(CallbackInfo ci){
        Mob mob=(Mob)(Object)this;
        for(HazeActor actor:HazeMod.ACTORS.values()){
            Fixation fixation=actor.targets.get(mob.getId());
            if(actor.active&&actor.dimension==mob.level().dimension()&&fixation!=null&&fixation.asleep(actor.abilities.time)){
                mob.getNavigation().stop();mob.setTarget(null);ci.cancel();return;
            }
        }
    }
}
