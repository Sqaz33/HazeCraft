package dev.local.haze.client;

import dev.local.haze.sim.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.*;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Supplier;

/** Creative-only controls grouped by the subsystem being checked. */
public final class DebugScreen extends Screen {
    private static final String[] TABS={"Здоровье","Движение","Умения","Оружие","Цели","Персонаж"};
    private static int lastTab;
    private int tab=lastTab,left,top,panelWidth,panelHeight,rowHeight,contentTop;
    private String notice="Изменения действуют сразу. F8 / Esc — вернуться в игру.";
    private final List<Binding> bindings=new ArrayList<>();
    private final List<Button> hazeButtons=new ArrayList<>();
    private final List<Button> targetButtons=new ArrayList<>();
    private final List<Caption> captions=new ArrayList<>();
    private record Binding(Button button,Supplier<String> label){}
    private record Caption(int row,String text){}
    public DebugScreen(){super(Component.literal("Haze — проверка"));}
    public static boolean allowed(){var p=Minecraft.getInstance().player;return p!=null&&p.isCreative();}
    @Override public boolean isPauseScreen(){return false;}
    @Override public boolean isInGameUi(){return true;}
    @Override public void renderBackground(GuiGraphics g,int x,int y,float partial){}
    @Override public void tick(){if(!allowed())onClose();}
    private ClientState state(){return HazeClient.own();}
    private String flag(boolean value){return value?"ВКЛ":"выкл";}
    @Override protected void init(){
        bindings.clear();hazeButtons.clear();targetButtons.clear();captions.clear();
        panelWidth=Math.min(560,width-12);panelHeight=Math.min(310,height-12);
        left=(width-panelWidth)/2;top=(height-panelHeight)/2;
        rowHeight=Math.max(16,Math.min(27,(panelHeight-104)/6));contentTop=top+78;
        var mode=addRenderableWidget(Button.builder(Component.literal(""),b->HazeClient.send(1)).bounds(left+panelWidth-110,top+6,100,18).build());
        bindings.add(new Binding(mode,()->HazeClient.active?"Haze → Стив":"Включить Haze"));
        int tabWidth=(panelWidth-20)/TABS.length;
        for(int i=0;i<TABS.length;i++){
            final int next=i;var button=addRenderableWidget(Button.builder(Component.literal(TABS[i]),b->{tab=lastTab=next;rebuildWidgets();}).bounds(left+10+i*tabWidth,top+29,tabWidth-2,18).build());
            button.active=i!=tab;
        }
        switch(tab){case 0->health();case 1->movement();case 2->abilities();case 3->weapon();case 4->targets();case 5->character();}
    }
    private Button button(int row,int column,int columns,Supplier<String> label,String hint,Runnable action,boolean hazeOnly){
        int cell=(panelWidth-20)/columns;
        var button=addRenderableWidget(Button.builder(Component.literal(label.get()),b->{
            if(!allowed()){onClose();return;}action.run();notice=hint;
        }).bounds(left+10+column*cell,contentTop+row*rowHeight,cell-4,rowHeight-3).build());
        button.setTooltip(Tooltip.create(Component.literal(hint)));bindings.add(new Binding(button,label));
        if(hazeOnly)hazeButtons.add(button);return button;
    }
    private void action(int row,int col,int columns,String label,String hint,DebugAction action,int value){
        var b=button(row,col,columns,()->label,hint,()->HazeClient.debug(action,value),true);
        if(action==DebugAction.TARGET_HEALTH||action==DebugAction.TARGET_STACKS||action==DebugAction.TARGET_SLEEP||action==DebugAction.TARGET_RESET)targetButtons.add(b);
    }
    private void number(int row,String label,int initial,DebugAction action){
        int labelWidth=Math.min(135,panelWidth/3),fieldWidth=Math.max(40,(panelWidth-20-labelWidth)/2);
        var field=addRenderableWidget(new EditBox(font,left+10+labelWidth,contentTop+row*rowHeight,fieldWidth-4,rowHeight-3,Component.literal(label)));
        field.setMaxLength(6);field.setFilter(s->s.matches("[0-9]*"));field.setValue(""+initial);captions.add(new Caption(row,label));
        var apply=addRenderableWidget(Button.builder(Component.literal("Применить"),b->{
            if(!allowed()||!HazeClient.active)return;
            try{int value=Integer.parseInt(field.getValue());HazeClient.debug(action,value);notice=label+": "+value;}
            catch(NumberFormatException e){notice="Введи целое число.";}
        }).bounds(left+10+labelWidth+fieldWidth,contentTop+row*rowHeight,panelWidth-24-labelWidth-fieldWidth,rowHeight-3).build());
        hazeButtons.add(apply);
        if(action==DebugAction.TARGET_STACKS)targetButtons.add(apply);
        if(action==DebugAction.MAX_HEALTH)field.setTooltip(Tooltip.create(Component.literal("От 1 до 1 000 000 HP. Исходный максимум учитывает уровень и предметы.")));
    }
    private void character(){
        number(0,"Уровень персонажа",state()==null?1:state().level,DebugAction.CHARACTER_LEVEL);
        action(4,0,1,"Событие cosmic veil","Отладочная подача исходного события входа в cosmic veil для Veil Walker",DebugAction.VEIL_EVENT,0);
        int max=new CharacterProgression().maximum();int[] levels={1,10,20,max};
        for(int i=0;i<levels.length;i++)action(1,i,levels.length,"Уровень "+levels[i],"Рост характеристик из таблицы Haze",DebugAction.CHARACTER_LEVEL,levels[i]);
        button(3,0,1,()->"Все текущие характеристики","Характеристики с сервера с учётом предметов и усилений",()->Minecraft.getInstance().setScreen(new CharacterStatsScreen(this)),true);
    }
    private void health(){
        int[] values={1,10,25,50,75,100};
        for(int i=0;i<values.length;i++)action(0,i,values.length,values[i]+"% HP","Выставить "+values[i]+"% здоровья",DebugAction.HEALTH_PERCENT,values[i]);
        number(1,"Точное здоровье",state()==null?730:(int)state().health,DebugAction.HEALTH_VALUE);
        number(2,"Максимум HP",state()==null?730:(int)state().maxHealth,DebugAction.MAX_HEALTH);
        button(3,0,1,()->"Зафиксировать HP: "+flag(state()!=null&&state().debugLockHealth),"Удерживать заданное HP: реген и лечение не мешают проверять HUD.",()->HazeClient.debug(DebugAction.LOCK_HEALTH,0),true);
        button(4,0,3,()->"−100 HP","Уменьшить здоровье на 100, минимум 1 HP",()->{if(state()!=null)HazeClient.debug(DebugAction.HEALTH_VALUE,(int)Math.max(1,state().health-100));},true);
        button(4,1,3,()->"Исходный максимум","Вернуть максимум здоровья Haze из файлов игры",()->HazeClient.debug(DebugAction.MAX_HEALTH,0),true);
        action(4,2,3,"Полное HP","Восстановить только здоровье",DebugAction.HEALTH_PERCENT,100);
    }
    private void movement(){
        for(int i=0;i<4;i++){
            final int amount=i;
            button(0,i,4,()->amount==3?"Стамина: MAX":"Стамина: "+amount,"Выставить стамину; для постоянного значения включи фиксацию.",()->DebugTools.stamina(amount==3&&HazeClient.motion!=null?HazeClient.motion.maxStamina():amount),true);
        }
        button(1,0,2,()->"Фиксация стамины: "+flag(DebugTools.lockStamina),"Удерживать выбранную стамину при расходе и регенерации",()->{
            if(!DebugTools.lockStamina&&HazeClient.motion!=null)DebugTools.staminaValue=HazeClient.motion.stamina;
            DebugTools.lockStamina=!DebugTools.lockStamina;
        },true);
        button(1,1,2,()->"Возд. лимиты: "+(DebugTools.unlimitedAir?"сняты":"обычные"),"Снимать лимиты воздушного прыжка, дэша и отскоков для проверки",()->DebugTools.unlimitedAir=!DebugTools.unlimitedAir,true);
        for(int i=0;i<3;i++){
            final int rate=i==0?0:i==1?1:10;
            button(2,i,3,()->"Реген ×"+rate+(DebugTools.regen==rate?" ✓":""),"×1 — исходный реген; ×10 — быстрый; ×0 — отключить.",()->DebugTools.regen=rate,true);
        }
        button(3,0,2,()->"Показатели в игре: "+flag(DebugTools.overlay),"Скорость, прыжки, стамина и клип остаются видимы после закрытия F8.",()->DebugTools.overlay=!DebugTools.overlay,false);
        button(3,1,2,()->"Сбросить пик скорости","Начать новое измерение максимальной горизонтальной скорости",()->DebugTools.peakSpeed=0,false);
        button(4,0,2,()->"Сбросить движение","Остановить движение и сбросить накопленные состояния",HazeClient::debugResetMovement,true);
        button(4,1,2,()->"Обычные настройки","Стамина без фиксации, воздушные лимиты включены, быстрый реген ×10",()->{DebugTools.lockStamina=DebugTools.unlimitedAir=false;DebugTools.regen=10;},true);
    }
    private void abilities(){
        String[] names={"1 · Sleep Dagger","2 · Smoke Bomb","3 · Fixation","4 · Bullet Dance"};
        int labelWidth=Math.min(132,panelWidth/3),buttonWidth=(panelWidth-24-labelWidth)/4;
        for(int i=0;i<4;i++){
            captions.add(new Caption(i,names[i]));
            for(int rank=0;rank<4;rank++){
                final int ability=i,tier=rank;
                var button=addRenderableWidget(Button.builder(Component.literal(""),b->{HazeClient.debug(DebugAction.ABILITY_RANK,ability*4+tier);notice=names[ability]+": уровень "+tier;})
                    .bounds(left+10+labelWidth+rank*buttonWidth,contentTop+i*rowHeight,buttonWidth-3,rowHeight-3).build());
                button.setTooltip(Tooltip.create(Component.literal("Точный уровень улучшения: "+rank+". Восстанавливает КД и заряды.")));
                bindings.add(new Binding(button,()->(state()!=null&&state().ranks[ability]==tier?"● ":"")+tier));hazeButtons.add(button);
            }
        }
        button(4,0,2,()->"Умения без КД: "+flag(state()!=null&&state().debugNoCooldowns),"Готовность умений без КД; длительности и задержки применения сохранены.",()->HazeClient.debug(DebugAction.NO_COOLDOWNS,0),true);
        action(4,1,2,"Сбросить КД / заряды","Разово восстановить готовность умений и заряды дыма",DebugAction.CLEAR_COOLDOWNS,0);
        action(5,0,1,"Остановить действующие умения","Остановить дым, Bullet Dance, бафф дыма и ожидающий нож",DebugAction.STOP_ABILITIES,0);
    }
    private void weapon(){
        int max=Retail.DATA.weapon().get("m_iClipSize").getAsInt();int[] values={0,1,max/2,max};
        for(int i=0;i<4;i++)action(0,i,4,values[i]+" патр.","Выставить патроны и остановить текущую перезарядку",DebugAction.AMMO,values[i]);
        number(1,"Точные патроны",state()==null?max:state().ammo,DebugAction.AMMO);
        button(2,0,1,()->"Бесконечные патроны: "+flag(state()!=null&&state().debugInfiniteAmmo),"Отключить расход патронов, сохранив темп стрельбы, разброс и отдачу",()->HazeClient.debug(DebugAction.INFINITE_AMMO,0),true);
        action(3,0,2,"Начать перезарядку","Начать обычную перезарядку неполного магазина",DebugAction.RELOAD,0);
        action(3,1,2,"Заполнить магазин","Восстановить только магазин и прервать перезарядку",DebugAction.AMMO,max);
    }
    private void targets(){
        action(0,0,3,"Один манекен","Создать и выбрать неподвижного манекена впереди; до 20 своих рядом",DebugAction.SPAWN_TARGET,0);
        action(0,1,3,"Группа из 5","Пять манекенов для Bullet Dance; препятствия не заменяются",DebugAction.SPAWN_GROUP,0);
        action(0,2,3,"Под прицелом","Выбрать моба под прицелом до 64 м; иначе ближайшего своего манекена",DebugAction.SELECT_TARGET,0);
        int[] stacks={0,10,25,50};
        for(int i=0;i<4;i++)action(1,i,4,stacks[i]+" стаков","Стаки выбранной цели, с исходным лимитом и временем жизни Fixation",DebugAction.TARGET_STACKS,stacks[i]);
        number(2,"Точные стаки",0,DebugAction.TARGET_STACKS);
        action(3,0,3,"Усыпить","Сон выбранной цели с текущим уровнем Sleep Dagger",DebugAction.TARGET_SLEEP,0);
        action(3,1,3,"Вылечить цель","Восстановить HP выбранного моба",DebugAction.TARGET_HEALTH,0);
        action(3,2,3,"Сбросить цель","Восстановить HP, убрать твои стаки, сон и замедление",DebugAction.TARGET_RESET,0);
        action(4,0,1,"Удалить своих манекенов рядом","Удалить только своих манекенов до 64 м; обычные мобы сохраняются",DebugAction.CLEAR_TARGETS,0);
    }
    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float partial){
        if(!allowed()){onClose();return;}
        g.fill(left,top,left+panelWidth,top+panelHeight,0xf0101820);g.fill(left,top,left+panelWidth,top+2,0xff79d4ad);
        g.drawString(font,title,left+10,top+10,0xffa6efd1,false);
        for(var binding:bindings)binding.button.setMessage(Component.literal(binding.label.get()));
        for(var button:hazeButtons)button.active=HazeClient.active;
        for(var button:targetButtons)button.active=HazeClient.active&&state()!=null&&state().debugTarget!=null;
        var s=state();String line1,line2;
        if(!HazeClient.active){line1="Включи Haze кнопкой справа сверху.";line2="Доступно только в креативе. Мир не приостанавливается.";}
        else if(tab==1){var lines=DebugTools.movementLines();line1=lines.get(0);line2=lines.size()>1?lines.get(1):"";}
        else if(tab==2&&s!=null){line1=String.format(Locale.ROOT,"КД: %.1f / %.1f / %.1f / %.1f с | дым: %d заряд(а)",s.cooldowns[0],s.cooldowns[1],s.cooldowns[2],s.cooldowns[3],s.smokeCharges);line2=String.format(Locale.ROOT,"Дым: %.1f с | Bullet Dance: %.1f с | действие: %s",s.smokeRemaining,s.danceRemaining,s.action);}
        else if(tab==3&&s!=null){line1=String.format(Locale.ROOT,"Патроны: %d | перезарядка: %.2f с | выстрелов: %d",s.ammo,s.reload,s.shots);line2=String.format(Locale.ROOT,"Разброс +%.3f° | отдача %.3f° | FPS %d",s.bloom,s.recoil,Minecraft.getInstance().getFps());}
        else if(tab==4&&s!=null){
            var target=s.debugTarget;
            line1=target==null?"Наведи прицел на моба и нажми «Под прицелом».":String.format(Locale.ROOT,"%s #%d | HP %.0f/%.0f | %.1f м",target.get("name").getAsString(),target.get("id").getAsInt(),target.get("health").getAsDouble(),target.get("maxHealth").getAsDouble(),target.get("distance").getAsDouble());
            line2=target==null?"Стаки, сон и лечение применяются только к выбранной цели.":"Fixation: "+target.get("stacks").getAsInt()+" | сон: "+(target.get("sleep").getAsBoolean()?"да":"нет");
        }else{line1=s==null?"Ожидание сервера...":String.format(Locale.ROOT,"HP: %.1f / %.0f (%.1f%%) | фиксация: %s",s.health,s.maxHealth,100*s.health/s.maxHealth,flag(s.debugLockHealth));line2="Проверяй край полосы, деления и цвет на разных значениях HP.";}
        g.drawString(font,font.plainSubstrByWidth(line1,panelWidth-20),left+10,top+52,0xffdbe8e3,false);
        g.drawString(font,font.plainSubstrByWidth(line2,panelWidth-20),left+10,top+64,0xffa7bbb2,false);
        for(var caption:captions)g.drawString(font,caption.text,left+10,contentTop+caption.row*rowHeight+(rowHeight-9)/2-1,0xffdbe8e3,false);
        g.drawString(font,font.plainSubstrByWidth(notice,panelWidth-20),left+10,top+panelHeight-13,0xff98aaa3,false);
        super.render(g,mouseX,mouseY,partial);
    }
}
