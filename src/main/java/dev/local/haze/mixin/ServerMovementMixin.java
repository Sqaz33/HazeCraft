package dev.local.haze.mixin;

import dev.local.haze.*;
import dev.local.haze.sim.V3;
import dev.local.haze.sim.movement.CapsuleMover;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.phys.AABB;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ServerMovementMixin {
    @Inject(method="isEntityCollidingWithAnythingNew",at=@At("HEAD"),cancellable=true)
    private void haze$validateCapsule(LevelReader level,Entity entity,AABB oldBounds,double x,double y,double z,CallbackInfoReturnable<Boolean> cir){
        if(!(entity instanceof Player p)||!HazeHooks.active(p))return;
        var shape=HazeCollisionWorld.create().shape(p.getPose()==Pose.CROUCHING);var world=new HazeCollisionWorld(p);V3 target=new V3(x,y,z);
        for(var box:world.boxes(shape.bounds(target)))if(CapsuleMover.contact(shape,target,box).separation()<-1e-5){cir.setReturnValue(true);return;}
        cir.setReturnValue(false);
    }
}
