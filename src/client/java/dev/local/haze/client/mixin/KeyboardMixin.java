package dev.local.haze.client.mixin;

import dev.local.haze.client.HazeClient;
import net.minecraft.client.KeyboardHandler;
import net.minecraft.client.input.KeyEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(KeyboardHandler.class)
public abstract class KeyboardMixin {
    @Inject(method="keyPress",at=@At("HEAD"))
    private void haze$capture(long window,int action,KeyEvent key,CallbackInfo ci) {
        HazeClient.keyboard(key.key(),action);
    }
}
