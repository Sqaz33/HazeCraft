package dev.local.haze.mixin;

import dev.local.haze.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMovementMixin {
    @Inject(method="move",at=@At("HEAD"),cancellable=true)
    private void haze$serverCapsule(MoverType type,Vec3 delta,CallbackInfo ci){
        if(!((Object)this instanceof Player p)||p.level().isClientSide()||type!=MoverType.PLAYER||!HazeHooks.active(p))return;
        var result=HazeCollisionWorld.create().resolve(HazeActor.of(p.position()),HazeActor.of(delta),p.getPose()==Pose.CROUCHING,new HazeCollisionWorld(p));
        Vec3 actual=HazeActor.mc(result.position()).subtract(p.position());p.setPos(HazeActor.mc(result.position()));
        p.horizontalCollision=Math.abs(actual.x-delta.x)>1e-7||Math.abs(actual.z-delta.z)>1e-7;
        p.verticalCollision=Math.abs(actual.y-delta.y)>1e-7;p.verticalCollisionBelow=p.verticalCollision&&delta.y<0;
        p.setOnGroundWithMovement(p.verticalCollisionBelow,p.horizontalCollision,actual);ci.cancel();
    }
}
