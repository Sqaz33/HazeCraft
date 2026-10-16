package dev.local.haze.client;

import dev.local.haze.HazeProtocol;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Creative-only live diagnostics and explicit testing actions. */
public final class DebugScreen extends Screen {
    private Button cooldownButton;
    private int left,top,panelWidth,panelHeight,buttonsTop;
    public DebugScreen(){super(Component.literal("Haze — отладка"));}
    public static boolean allowed(){var p=Minecraft.getInstance().player;return p!=null&&p.isCreative();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    @Override public void tick(){if(!allowed())onClose();}
    @Override protected void init(){
        panelWidth=Math.min(500,width-12);panelHeight=Math.min(270,height-12);
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;buttonsTop=top+panelHeight-108;
        button(0,"Стив / Haze",()->HazeClient.send(1));
        button(1,"Здоровье, патроны, КД",()->HazeClient.send(HazeProtocol.DEBUG_REFILL));
        button(2,"Восстановить стамину",()->{if(HazeClient.motion!=null)HazeClient.motion.stamina=HazeClient.motion.maxStamina();});
        button(3,"Сбросить движение",HazeClient::debugResetMovement);
        button(4,"Создать манекен",()->HazeClient.send(HazeProtocol.DEBUG_TARGET));
        button(5,"Убрать свои манекены",()->HazeClient.send(HazeProtocol.DEBUG_CLEAR_TARGETS));
        cooldownButton=button(6,"Умения без КД: выкл.",()->HazeClient.send(HazeProtocol.DEBUG_NO_COOLDOWNS));
        button(7,"Закрыть",this::onClose);
    }
    private Button button(int index,String label,Runnable action){
        int w=(panelWidth-28)/2;
        return addRenderableWidget(Button.builder(Component.literal(label),b->{if(allowed())action.run();else onClose();})
            .bounds(left+10+(index%2)*(w+8),buttonsTop+(index/2)*23,w,20).build());
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        if(!allowed()){onClose();return;}
        g.fill(left,top,left+panelWidth,top+panelHeight,0xed101820);
        g.fill(left,top,left+panelWidth,top+2,0xff79d4ad);
        g.drawString(font,title,left+10,top+10,0xffa6efd1,false);
        ClientState state=HazeClient.own();var motion=HazeClient.motion;
        cooldownButton.setMessage(Component.literal("Умения без КД: "+(state!=null&&state.debugNoCooldowns?"вкл.":"выкл.")));
        List<String> lines=new ArrayList<>();
        lines.add("Режим: "+(HazeClient.active?"Haze":"Стив")+"   FPS: "+Minecraft.getInstance().getFps());
        if(motion!=null){
            lines.add(String.format(Locale.ROOT,"Движение: %s   земля: %s   стена: %s",motion.mode,motion.grounded,motion.wall));
            lines.add(String.format(Locale.ROOT,"Скорость: %.2f м/с   вверх: %.2f м/с",motion.velocity.horizontal(),motion.velocity.y()));
            lines.add(String.format(Locale.ROOT,"Стамина: %.2f / %.0f   прыжок: %s #%d",motion.stamina,motion.maxStamina(),motion.jumpKind,motion.jumpSerial));
            lines.add("Возд. прыжок: "+motion.airJumpUsed+"   возд. дэш: "+motion.airDashUsed+"   стен: "+motion.wallJumps);
        }
        if(state!=null){
            lines.add(String.format(Locale.ROOT,"HP: %.0f / %.0f   патроны: %d   выстрелов: %d",state.health,state.maxHealth,state.ammo,state.shots));
            lines.add(String.format(Locale.ROOT,"КД умений:  %.1f / %.1f / %.1f / %.1f с",state.cooldowns[0],state.cooldowns[1],state.cooldowns[2],state.cooldowns[3]));
        }
        String clip=HazeRenderer.currentClip;lines.add("Анимация: "+clip.substring(clip.lastIndexOf('/')+1));
        int y=top+27;int lineHeight=Math.max(9,Math.min(13,(buttonsTop-y-5)/Math.max(1,lines.size())));
        for(String line:lines){if(y+9>buttonsTop-3)break;g.drawString(font,font.plainSubstrByWidth(line,panelWidth-20),left+10,y,0xffdbe8e3,false);y+=lineHeight;}
        g.drawString(font,"F8 / Esc — закрыть. Мир продолжает работать.",left+10,top+panelHeight-12,0xff98aaa3,false);
        super.render(g,mouseX,mouseY,partial);
    }
}
