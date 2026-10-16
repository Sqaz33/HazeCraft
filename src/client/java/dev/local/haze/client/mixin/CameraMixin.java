package dev.local.haze.client.mixin;

import dev.local.haze.client.*;
import dev.local.haze.sim.*;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Inject(method="setup",at=@At("TAIL"))
    private void haze$camera(Level level,Entity e,boolean third,boolean front,float partial,CallbackInfo ci) {
        if(!HazeClient.active||!HazeClient.active(e.getUUID()))return;
        HazeView.updateCrouch(partial);
        Retail d=Retail.DATA;HazeView.Parameters view=HazeView.parameters(partial);
        double height=view.height(),back=view.back(),side=view.side();
        ClientState state=HazeClient.own();float yaw=e.getYRot(partial),pitch=e.getXRot(partial)-(float)(state==null?0:state.recoil);
        V3 forward=V3.look(yaw,pitch),right=V3.look(yaw+90,0);
        Vec3 pivot=new Vec3(e.xOld+(e.getX()-e.xOld)*partial,e.yOld+(e.getY()-e.yOld)*partial+height,e.zOld+(e.getZ()-e.zOld)*partial);
        Vec3 offset=new Vec3(-forward.x()*back-right.x()*side,-forward.y()*back,-forward.z()*back-right.z()*side);
        Vec3 target=pivot.add(offset);
        BlockHitResult hit=level.clip(new ClipContext(pivot,target,ClipContext.Block.VISUAL,ClipContext.Fluid.NONE,e));
        if(hit.getType()!=HitResult.Type.MISS)target=hit.getLocation().subtract(offset.normalize().scale(d.nativeFloat("citadel_camera_hard_trace_radius")*Retail.SOURCE_UNIT));
        CameraAccessor accessor=(CameraAccessor)this;accessor.haze$position(target);accessor.haze$rotation(yaw,pitch);
    }
}
