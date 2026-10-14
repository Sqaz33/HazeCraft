package dev.local.haze.client.mixin;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.*;
@Mixin(MouseHandler.class)
public interface MouseAccessor {
    @Accessor("accumulatedDX")void haze$dx(double value);
    @Accessor("accumulatedDY")void haze$dy(double value);
    @Invoker("turnPlayer")void haze$turn(double elapsed);
}
