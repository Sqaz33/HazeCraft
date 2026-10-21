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
        if(source.getEntity()!=null)for(var effectOwner:dev.local.haze.HazeMod.ACTORS.values())if(effectOwner.active)amount*=effectOwner.outgoingItemMultiplier(source.getEntity().getId(),source.is(net.minecraft.tags.DamageTypeTags.IS_FIRE)||source.getDirectEntity()!=source.getEntity()&&!(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.arrow.AbstractArrow));
        var actor=dev.local.haze.HazeMod.ACTORS.get(((LivingEntity)(Object)this).getUUID());
        if(actor==null||!actor.active)return amount;
        if((Object)this instanceof net.minecraft.server.level.ServerPlayer p)return actor.incomingItemDamage(p,source,amount);
        return amount;
    }
    @ModifyVariable(method="heal",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float haze$itemHealing(float amount){if(!((LivingEntity)(Object)this).level().isClientSide())for(var actor:dev.local.haze.HazeMod.ACTORS.values())if(actor.active)amount*=actor.healMultiplier(((LivingEntity)(Object)this).getId());return amount;}
    @Inject(method="jumpFromGround",at=@At("HEAD"),cancellable=true)
    private void haze$ownedJump(CallbackInfo ci) {
        if((Object)this instanceof Player p&&p.level().isClientSide()&&HazeHooks.active(p))ci.cancel();
    }
    @Inject(method="maxUpStep",at=@At("HEAD"),cancellable=true)
    private void haze$retailStep(CallbackInfoReturnable<Float> cir){if((Object)this instanceof Player p&&HazeHooks.active(p))cir.setReturnValue((float)(Retail.DATA.model.get("m_flStepHeight").getAsDouble()*Retail.SOURCE_UNIT));}
}
