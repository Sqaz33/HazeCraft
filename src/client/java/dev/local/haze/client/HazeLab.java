package dev.local.haze.client;

import net.minecraft.client.Minecraft;

/** Client scene harness is enabled only by the development launch property. */
public final class HazeLab {
    public static volatile int held;
    public static void tick(Minecraft mc) {
        if(!Boolean.getBoolean("haze.lab"))return;
        if(mc.player.tickCount%100==0)dev.local.haze.HazeMod.LOG.info("HAZE_LAB frame={} clip={} mode={}",HazeRenderer.renderedFrames,HazeRenderer.currentClip,HazeClient.motion==null?"none":HazeClient.motion.mode);
    }
}
