package dev.local.haze.client.mixin;

import dev.local.haze.client.HazeClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
    @Inject(method="keyPress",at=@At("HEAD"),cancellable=true)
    private void haze$capture(long window,int action,KeyEvent key,CallbackInfo ci) {
        if(HazeClient.debugKey(window,key.key(),action)){ci.cancel();return;}
        if(HazeClient.shopKey(window,key.key(),action)){ci.cancel();return;}
        HazeClient.keyboard(key.key(),action);
        if(HazeClient.ownsMeleeKey(window,key.key()))ci.cancel();
    }
}
