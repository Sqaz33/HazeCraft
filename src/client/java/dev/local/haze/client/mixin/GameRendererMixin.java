package dev.local.haze.client.mixin;

import dev.local.haze.client.HazeClient;
import dev.local.haze.client.HazeView;
import dev.local.haze.sim.*;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Inject(method="getFov",at=@At("HEAD"),cancellable=true)
    private void haze$fov(Camera camera,float partial,boolean useSetting,CallbackInfoReturnable<Float> cir){
        if(!HazeClient.active)return;
        cir.setReturnValue(HazeView.verticalFov(partial));
    }
    @Inject(method="bobView",at=@At("HEAD"),cancellable=true)
    private void haze$bob(PoseStack pose,float partial,CallbackInfo ci){if(HazeClient.active)ci.cancel();}
}
