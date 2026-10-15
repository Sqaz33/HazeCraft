package dev.local.haze.mixin;

import dev.local.haze.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.entity.MoverType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method="maybeBackOffFromEdge",at=@At("HEAD"),cancellable=true)
    private void haze$slideOverEdge(Vec3 movement,MoverType type,CallbackInfoReturnable<Vec3> cir) {
        Player p=(Player)(Object)this;
        if(p.level().isClientSide()&&HazeHooks.active(p))cir.setReturnValue(movement);
    }
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void haze$travel(Vec3 input,CallbackInfo ci) {
        Player p=(Player)(Object)this;
        if(p.level().isClientSide() && HazeHooks.client!=null && HazeHooks.client.travel(p)) ci.cancel();
    }
}
