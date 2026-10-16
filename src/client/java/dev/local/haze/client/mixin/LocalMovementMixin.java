package dev.local.haze.client.mixin;

import dev.local.haze.client.HazeClient;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Abilities;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Mixin(LocalPlayer.class)
public abstract class LocalMovementMixin {
    @Shadow private int autoJumpTime;
    @Inject(method="aiStep",at=@At("HEAD"))
    private void haze$clearAutoJump(CallbackInfo ci) { if(HazeClient.active)autoJumpTime=0; }
    @Redirect(method="aiStep",at=@At(value="FIELD",target="Lnet/minecraft/world/entity/player/Abilities;mayfly:Z",opcode=180))
    private boolean haze$nativeAirJumpInsteadOfFlight(Abilities abilities){return !HazeClient.active&&abilities.mayfly;}
    @Redirect(method="aiStep",at=@At(value="FIELD",target="Lnet/minecraft/world/entity/player/Abilities;flying:Z",opcode=180))
    private boolean haze$nativeGravity(Abilities abilities){return !HazeClient.active&&abilities.flying;}
    @Inject(method="isAutoJumpEnabled",at=@At("HEAD"),cancellable=true)
    private void haze$autoJump(CallbackInfoReturnable<Boolean> cir) { if(HazeClient.active)cir.setReturnValue(false); }
    @Inject(method="moveTowardsClosestSpace",at=@At("HEAD"),cancellable=true)
    private void haze$pushOut(double x,double z,CallbackInfo ci) { if(HazeClient.active)ci.cancel(); }
}
