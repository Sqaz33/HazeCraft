package dev.local.haze;

import net.minecraft.world.entity.player.Player;

/** Sided bridge: common bytecode never loads client classes on dedicated servers. */
public final class HazeHooks {
    public interface ClientMotion { boolean travel(Player player); boolean active(Player player); }
    public static ClientMotion client;
    public static boolean active(Player player) {
        if(player.level().isClientSide()) return client!=null && client.active(player);
        HazeActor actor=HazeMod.ACTORS.get(player.getUUID());return actor!=null && actor.active;
    }
}
