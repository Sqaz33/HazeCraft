package dev.local.haze.client;

import dev.local.haze.sim.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import java.util.*;

final class DebugTools {
    static boolean overlay,lockStamina,unlimitedAir;
    static double staminaValue,regen=10,peakSpeed;
    static void clear(){overlay=lockStamina=unlimitedAir=false;staminaValue=peakSpeed=0;regen=10;}
    static void permissions(){
        if(!DebugScreen.allowed()){
            clear();if(HazeClient.motion!=null)HazeClient.motion.setStaminaRegenMultiplier(10);
        }
    }
    static void stamina(double value){
        if(!DebugScreen.allowed()||HazeClient.motion==null)return;
        staminaValue=Math.max(0,Math.min(HazeClient.motion.maxStamina(),value));HazeClient.motion.stamina=staminaValue;
    }
    static void beforeStep(){
        if(!DebugScreen.allowed()||HazeClient.motion==null)return;
        var m=HazeClient.motion;m.setStaminaRegenMultiplier(regen);
        if(lockStamina)m.stamina=staminaValue;
        if(unlimitedAir){m.airJumpUsed=m.airDashUsed=m.downDashUsed=false;m.wallJumps=0;}
    }
    static void afterStep(){
        if(!DebugScreen.allowed()||HazeClient.motion==null)return;
        if(lockStamina)HazeClient.motion.stamina=staminaValue;
        peakSpeed=Math.max(peakSpeed,HazeClient.motion.velocity.horizontal());
    }
    static List<String> movementLines(){
        var m=HazeClient.motion;if(m==null)return List.of("Включи Haze для проверки движения");
        String clip=HazeRenderer.currentClip;
        return List.of(
            String.format(Locale.ROOT,"%s | земля %s | присед %s | стена %s",m.mode,m.grounded,m.crouchedPose(),m.wall),
            String.format(Locale.ROOT,"Скорость %.2f | Y %+.2f | пик %.2f м/с",m.velocity.horizontal(),m.velocity.y(),peakSpeed),
            String.format(Locale.ROOT,"Стамина %.2f/%.0f | прыжок %s #%d",m.stamina,m.maxStamina(),m.jumpKind,m.jumpSerial),
            "Возд. прыжок "+(m.airJumpUsed?"использован":"готов")+" | дэш "+(m.airDashUsed?"использован":"готов")+" | стен "+m.wallJumps,
            "Дэш "+m.dashFacing+String.format(Locale.ROOT," | вектор %.2f, %.2f, %.2f",m.dashDirection.x(),m.dashDirection.y(),m.dashDirection.z()),
            "Мили: "+m.melee.phase+" / "+m.melee.type+String.format(Locale.ROOT," | фаза %.2f | КД %.2f",m.melee.age(),m.melee.cooldown()),
            "Клип: "+clip.substring(clip.lastIndexOf('/')+1));
    }
    static void renderOverlay(GuiGraphics g){
        if(!overlay||!DebugScreen.allowed()||Minecraft.getInstance().screen instanceof DebugScreen)return;
        var font=Minecraft.getInstance().font;var lines=new ArrayList<>(movementLines());
        var s=HazeClient.own();
        if(s!=null)lines.add(String.format(Locale.ROOT,"HP %.0f/%.0f | патроны %d | FPS %d",s.health,s.maxHealth,s.ammo,Minecraft.getInstance().getFps()));
        int width=lines.stream().mapToInt(font::width).max().orElse(200);
        g.fill(5,5,width+17,13+lines.size()*11,0xb0101820);
        for(int i=0;i<lines.size();i++)g.drawString(font,lines.get(i),10,10+i*11,0xffdbe8e3,false);
    }
}
