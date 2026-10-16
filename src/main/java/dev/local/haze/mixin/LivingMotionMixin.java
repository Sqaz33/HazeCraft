package dev.local.haze.mixin;
import dev.local.haze.HazeHooks;
import dev.local.haze.sim.Retail;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntity.class)
public abstract class LivingMotionMixin {
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float haze$itemResistance(float amount,net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float original){
        var actor=dev.local.haze.HazeMod.ACTORS.get(((LivingEntity)(Object)this).getUUID());
        if(actor==null||!actor.active)return amount;
        String key=source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)?"BulletResist":source.getDirectEntity() instanceof LivingEntity?"MeleeResistPercent":"";
        return (float)(amount*(1-actor.items.reduction(key)));
    }
    @Inject(method="jumpFromGround",at=@At("HEAD"),cancellable=true)
    private void haze$ownedJump(CallbackInfo ci) {
        if((Object)this instanceof Player p&&p.level().isClientSide()&&HazeHooks.active(p))ci.cancel();
    }
    @Inject(method="maxUpStep",at=@At("HEAD"),cancellable=true)
    private void haze$retailStep(CallbackInfoReturnable<Float> cir){if((Object)this instanceof Player p&&HazeHooks.active(p))cir.setReturnValue((float)(Retail.DATA.model.get("m_flStepHeight").getAsDouble()*Retail.SOURCE_UNIT));}
}
