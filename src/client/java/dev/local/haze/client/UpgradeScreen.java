package dev.local.haze.client;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public final class UpgradeScreen extends Screen {
    public UpgradeScreen(){super(Component.literal("Haze — улучшения"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float f){}
    @Override public void render(GuiGraphics g,int x,int y,float f){HazeHud.upgrades(g,x,y);}
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
        int index=HazeHud.upgradeAt(e.x(),e.y());
        if(e.button()==0&&index>=0){HazeClient.send(10+index);return true;}return false;
    }
}
