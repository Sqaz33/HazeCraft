package dev.local.haze.mixin;
import dev.local.haze.HazeHooks;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class EntitySoundMixin {
    @Inject(method="playStepSound",at=@At("HEAD"),cancellable=true)
    private void haze$retailFootstep(BlockPos pos,BlockState state,CallbackInfo ci){if((Object)this instanceof Player p&&HazeHooks.active(p))ci.cancel();}
}
