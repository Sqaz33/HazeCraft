package dev.local.haze.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.Locale;

/** Values are sent by the authoritative actor, including its current timed buffs. */
public final class CharacterStatsScreen extends Screen {
    private final Screen previous;
    public CharacterStatsScreen(Screen previous){super(Component.literal("Haze — текущие характеристики"));this.previous=previous;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    @Override protected void init(){addRenderableWidget(Button.builder(Component.literal("Назад"),b->onClose()).bounds(width/2-50,height-28,100,20).build());}
    @Override public void onClose(){minecraft.setScreen(previous);}
    @Override public void render(GuiGraphics g,int mx,int my,float delta){
        var state=HazeClient.own();if(state==null||!state.active){onClose();return;}
        int panel=Math.min(width-12,660),left=(width-panel)/2;
        g.fill(left,8,left+panel,height-34,0xf0101820);g.drawCenteredString(font,title,width/2,18,0xffa6efd1);
        int rows=(state.characterStats.size()+1)/2,spacing=Math.min(21,Math.max(10,(height-85)/Math.max(1,rows))),index=0;
        for(var entry:state.characterStats.entrySet()){
            int column=index/rows,row=index%rows;int x=left+12+column*(panel/2),y=40+row*spacing;
            String value=String.format(Locale.ROOT,"%.2f",entry.getValue().getAsDouble()).replaceAll("\\.?0+$","");
            g.drawString(font,font.plainSubstrByWidth(entry.getKey(),panel/2-80),x,y,0xffb9c9c0,false);
            g.drawString(font,value,x+panel/2-24-font.width(value),y,0xffffefd7,false);index++;
        }
        super.render(g,mx,my,delta);
    }
}
