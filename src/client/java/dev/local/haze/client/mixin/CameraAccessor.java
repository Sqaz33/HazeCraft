package dev.local.haze.client.mixin;

import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Camera.class)
public interface CameraAccessor {
    @Invoker("setPosition")void haze$position(Vec3 pos);
    @Invoker("setRotation")void haze$rotation(float yaw,float pitch);
}
