package dev.local.haze.client.mixin;

import dev.local.haze.client.HazeClient;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
    private void haze$attack(CallbackInfoReturnable<Boolean> cir){if(HazeClient.active)cir.setReturnValue(false);}
    @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true)
    private void haze$continue(boolean held,CallbackInfo ci){if(HazeClient.active)ci.cancel();}
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void haze$use(CallbackInfo ci){if(HazeClient.active)ci.cancel();}
}
