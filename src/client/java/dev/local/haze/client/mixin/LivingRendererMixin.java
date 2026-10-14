package dev.local.haze.client.mixin;

import dev.local.haze.client.HazeClient;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingRendererMixin {
    @Unique private static final Set<LivingEntityRenderState> haze$replaced=Collections.newSetFromMap(new WeakHashMap<>());
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void haze$tag(LivingEntity entity,LivingEntityRenderState state,float partial,CallbackInfo ci){
        if(entity instanceof Player&&HazeClient.active(entity.getUUID()))haze$replaced.add(state);else haze$replaced.remove(state);
    }
    @Inject(method="submit",at=@At("HEAD"),cancellable=true)
    private void haze$replace(LivingEntityRenderState state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera,CallbackInfo ci){if(haze$replaced.contains(state))ci.cancel();}
}
