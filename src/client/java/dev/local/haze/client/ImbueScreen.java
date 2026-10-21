package dev.local.haze.client;
import dev.local.haze.sim.*;
import dev.local.haze.HazeProtocol;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
public final class ImbueScreen extends Screen {
    private final Screen previous;private final ItemCatalog.Item item;
    public ImbueScreen(Screen previous,ItemCatalog.Item item){super(Component.literal("Выбрать умение: "+item.name()));this.previous=previous;this.item=item;}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float p){}
    @Override protected void init(){String[] names={"1 — Sleep Dagger","2 — Smoke Bomb","4 — Bullet Dance"};int[] indices={0,1,3};
        for(int i=0;i<indices.length;i++){int ability=indices[i];var button=addRenderableWidget(Button.builder(Component.literal(names[i]),b->{ClientPlayNetworking.send(new HazeProtocol.Imbue(item.id(),ability));onClose();}).bounds(width/2-110,height/2-20+i*24,220,20).build());button.active=ability!=3||!item.data().get("m_TargetAbilityEffectsToApply").getAsString().contains("NON_ULT");}}
    @Override public void onClose(){minecraft.setScreen(previous);}
    @Override public void render(GuiGraphics g,int x,int y,float p){g.fill(width/2-160,height/2-60,width/2+160,height/2+85,0xf0101820);g.drawCenteredString(font,title,width/2,height/2-45,0xffffefd7);super.render(g,x,y,p);}
}
