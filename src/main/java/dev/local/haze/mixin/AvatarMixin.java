package dev.local.haze.mixin;

import dev.local.haze.HazeHooks;
import dev.local.haze.sim.Retail;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Avatar.class)
public abstract class AvatarMixin {
    @Inject(method="getDefaultDimensions",at=@At("HEAD"),cancellable=true)
    private void haze$dimensions(Pose pose,CallbackInfoReturnable<EntityDimensions> cir){
        if(!((Object)this instanceof Player p)||!HazeHooks.active(p))return;
        Retail d=Retail.DATA;boolean crouch=pose==Pose.CROUCHING;
        double radius=d.model.get("m_flCollisionRadius").getAsDouble(),step=d.model.get("m_flStepHeight").getAsDouble();
        float width=(float)(2*radius*Retail.SOURCE_UNIT);
        float height=(float)((Math.max(crouch?step:d.model.get("m_flCollisionHeight").getAsDouble(),step+radius)+radius)*Retail.SOURCE_UNIT);
        float eye=(float)(d.camera.get(crouch?"m_flCameraHeightCrouching":"m_flCameraHeightStanding").getAsDouble()*Retail.SOURCE_UNIT);
        cir.setReturnValue(EntityDimensions.scalable(width,height).withEyeHeight(eye));
    }
}
