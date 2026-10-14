package dev.local.haze.client.mixin;

import dev.local.haze.client.HazeClient;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class GuiMixin {
    @Inject(method={"renderCrosshair","renderHotbarAndDecorations","renderEffects"},at=@At("HEAD"),cancellable=true)
    private void haze$hideVanilla(GuiGraphics gui,DeltaTracker delta,CallbackInfo ci){if(HazeClient.active)ci.cancel();}
}
