package dev.local.haze.client.mixin;

import dev.local.haze.client.HazeClient;
import dev.local.haze.sim.*;
import net.minecraft.client.*;
import com.google.gson.JsonObject;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public abstract class MouseMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;
    @Inject(method="turnPlayer",at=@At("HEAD"),cancellable=true)
    private void haze$sourceMouse(double elapsed,CallbackInfo ci){
        if(!HazeClient.active)return;Minecraft mc=Minecraft.getInstance();if(mc.player==null)return;
        JsonObject input=Retail.DATA.root.getAsJsonObject("localInputSettings");
        double sensitivity=Retail.number(input.get("sensitivity"));
        if(HazeClient.controls.down(Controls.AIM))sensitivity*=Retail.number(input.get("zoom_sensitivity_ratio"));
        // Player.turn multiplies its arguments by 0.15 degrees. Preserve raw Source counts.
        mc.player.turn(accumulatedDX*sensitivity*Retail.number(input.get("m_yaw"))/0.15,
            accumulatedDY*sensitivity*Retail.number(input.get("m_pitch"))/0.15);
        ci.cancel();
    }
}
