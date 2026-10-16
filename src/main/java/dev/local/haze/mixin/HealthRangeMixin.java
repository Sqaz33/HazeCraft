package dev.local.haze.mixin;
import net.minecraft.world.entity.ai.attributes.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(RangedAttribute.class)
public abstract class HealthRangeMixin {
    @Inject(method="sanitizeValue",at=@At("HEAD"),cancellable=true)
    private void haze$itemHealth(double value,CallbackInfoReturnable<Double> result){
        if(Attributes.MAX_HEALTH!=null&&(Object)this==Attributes.MAX_HEALTH.value()&&Double.isFinite(value)&&value>1024)result.setReturnValue(Math.min(value,1000000));
    }
}
