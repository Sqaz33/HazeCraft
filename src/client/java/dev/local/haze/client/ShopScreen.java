package dev.local.haze.client;

import dev.local.haze.sim.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import java.util.*;

/** Original catalogue/icons/fonts in Panorama's 1080p coordinate space. */
public final class ShopScreen extends Screen {
    private static final int[] COLORS={0xffce9a56,0xff88ba53,0xffb58bcb};
    private static final String[] CATEGORIES={"EItemSlotType_WeaponMod","EItemSlotType_Armor","EItemSlotType_Tech"};
    private final List<Card> cards=new ArrayList<>();
    private record Card(String id,float x,float y,float w,float h){}
    private String selected="upgrade_clip_size";
    private float scroll,left,scale,maxScroll;
    private EditBox search;
    public ShopScreen(){super(Component.literal("Магазин"));}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float f){}
    @Override protected void init(){
        scale=height/1080F;left=(width/scale-1810)/2;
        search=addRenderableWidget(new EditBox(font,Math.round((left+660)*scale),Math.round(65*scale),Math.round(1110*scale),Math.max(10,Math.round(32*scale)),Component.literal("Поиск предметов")));
        search.setHint(Component.literal("Поиск предметов"));search.setMaxLength(80);search.setResponder(s->scroll=0);
    }
    private void text(GuiGraphics g,String value,float x,float y,float size,String face,int color){HazeHud.text(g,value,x,y,size,face,color,false);}
    static void icon(GuiGraphics g,ItemCatalog.Item item,float x,float y,float size){g.blit(Identifier.fromNamespaceAndPath("haze","retail/"+item.icon()),(int)x,(int)y,(int)(x+size),(int)(y+size),0,1,0,1);}
    private ItemInventory inventory(){var s=HazeClient.own();return s==null?new ItemInventory():s.itemInventory;}
    @Override public void tick(){if(!HazeClient.active)onClose();}
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        var inventory=inventory();float mx=mouseX/scale,my=mouseY/scale;
        g.pose().pushMatrix();g.pose().scale(scale,scale);
        g.fill((int)left,20,(int)left+1810,1060,0xe910130d);g.fill((int)left+650,20,(int)left+1810,1060,0xf0202621);
        text(g,"МАГАЗИН",left+60,55,52,"oracle",0xffffefd7);text(g,"∞ ДУШ",left+60,118,38,"oracle",0xffa4d8ce);text(g,"B / Esc — закрыть",left+60,170,22,"radiance",0xffa8afa7);
        String query=search.getValue().toLowerCase(Locale.ROOT);cards.clear();g.enableScissor((int)left+650,130,(int)left+1810,880);float y=140-scroll;
        for(int tier=1;tier<=4;tier++){
            text(g,""+(800<<(tier-1)),left+665,y,26,"oracle",0xffd0c7b8);y+=35;int maxRows=0;
            for(int cat=0;cat<3;cat++){
                final int t=tier,c=cat;
                var items=ItemCatalog.ITEMS.values().stream().filter(i->i.tier()==t&&i.category().equals(CATEGORIES[c])&&(i.name().toLowerCase(Locale.ROOT).contains(query)||i.id().contains(query))).toList();maxRows=Math.max(maxRows,(items.size()+3)/4);
                for(int index=0;index<items.size();index++){
                    var item=items.get(index);float x=left+660+cat*380+(index%4)*91,cy=y+(index/4)*108;if(cy+100<130||cy>880)continue;
                    boolean owned=inventory.owned.contains(item.id()),hover=mx>=x&&mx<x+84&&my>=cy&&my<cy+101;
                    g.fill((int)x,(int)cy,(int)x+84,(int)cy+80,owned?0xffd0dfaa:COLORS[cat]);icon(g,item,x+5,cy+3,74);
                    if(ItemInventory.needsImbue(item))text(g,"СКМ — выбрать усиливаемое умение",left+60,760,19,"radiance",0xffa4d8ce);
            if(item.active())text(g,"◆",x+66,cy+2,16,"radiance",0xff10130d);
                    text(g,owned?"КУПЛЕНО":""+inventory.price(item.id()),x+3,cy+81,15,"radiance",owned?0xffb9db9b:0xffdfd7c7);cards.add(new Card(item.id(),x,Math.max(130,cy),84,Math.min(880,cy+101)-Math.max(130,cy)));
                    if(hover){selected=item.id();g.fill((int)x,(int)cy,(int)x+84,(int)cy+2,0xffffefd7);}
                }
            }y+=maxRows*108+20;
        }
        maxScroll=Math.max(0,y+scroll-880);g.disableScissor();String[] names={"ОРУЖИЕ","ЖИВУЧЕСТЬ","ДУХ"};for(int c=0;c<3;c++)text(g,names[c],left+660+c*380,110,22,"oracle",COLORS[c]);
        var item=ItemCatalog.ITEMS.get(selected);
        if(item!=null){
            icon(g,item,left+60,230,128);text(g,item.name(),left+60,380,29,"oracle",0xffffefd7);text(g,""+inventory.price(item.id())+" ДУШ",left+60,420,23,"radiance",0xffa4d8ce);
            int row=0;for(var stat:item.stats().entrySet())if(stat.getValue()!=0){text(g,String.format(Locale.ROOT,"%+.1f %s%s",stat.getValue(),ItemCatalog.label(stat.getKey()),ItemCatalog.APPLIED.contains(stat.getKey())?"":" (не действует)"),left+60,465+row++*26,18,"radiance",0xffd4d7cb);if(row>=9)break;}
            if(ItemInventory.needsImbue(item))text(g,"СКМ — выбрать усиливаемое умение",left+60,760,19,"radiance",0xffa4d8ce);
            if(item.active())text(g,"Применение: Z / X / C / V по порядку покупки",left+60,735,19,"radiance",0xffe2bb79);
            if(!item.components().isEmpty()){text(g,"КОМПОНЕНТЫ",left+60,780,20,"oracle",0xffd4d7cb);int n=0;for(String component:item.components()){var base=ItemCatalog.ITEMS.get(component);if(base!=null)icon(g,base,left+60+n++*70,815,60);}}
        }
        text(g,"ЛКМ — купить / улучшить    ПКМ — продать",left+660,900,21,"radiance",0xffb7c0b0);
        for(int slot=0;slot<12;slot++){
            float x=left+660+slot*91;g.fill((int)x,940,(int)x+82,1022,slot<inventory.capacity?0xff464a3f:0xff252a24);
            if(slot<inventory.owned.size()){var owned=ItemCatalog.ITEMS.get(inventory.owned.get(slot));icon(g,owned,x+3,943,76);cards.add(new Card(owned.id(),x,940,82,82));}
            else if(slot>=inventory.capacity)text(g,"⌑",x+30,963,30,"radiance",0xff8b9084);
        }
        var state=HazeClient.own();if(state!=null)text(g,state.shopMessage,left+60,1015,19,"radiance",0xffb7c0b0);
        g.pose().popMatrix();super.render(g,mouseX,mouseY,partial);
    }
    @Override public boolean mouseClicked(MouseButtonEvent event,boolean doubleClick){float x=(float)event.x()/scale,y=(float)event.y()/scale;for(var card:cards)if(x>=card.x&&x<card.x+card.w&&y>=card.y&&y<card.y+card.h){var item=ItemCatalog.ITEMS.get(card.id);if(event.button()==2&&inventory().owned.contains(card.id)&&ItemInventory.needsImbue(item)){minecraft.setScreen(new ImbueScreen(this,item));return true;}HazeClient.shop(card.id,event.button()==1);if(event.button()==0&&ItemInventory.needsImbue(item))minecraft.setScreen(new ImbueScreen(this,item));return true;}return super.mouseClicked(event,doubleClick);}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){scroll=Math.max(0,Math.min(maxScroll,scroll-(float)vertical*100));return true;}
}
