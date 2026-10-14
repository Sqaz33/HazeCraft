package dev.local.haze.mixin;

import dev.local.haze.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void haze$travel(Vec3 input,CallbackInfo ci) {
        Player p=(Player)(Object)this;
        if(p.level().isClientSide() && HazeHooks.client!=null && HazeHooks.client.travel(p)) ci.cancel();
    }
}
