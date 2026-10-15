package dev.local.haze.client;

import dev.local.haze.sim.*;
import dev.local.haze.sim.movement.StaminaDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.*;
import java.util.*;

/** Panorama's 1080p coordinate space, original assets, original font faces and scoped style declarations. */
public final class HazeHud {
    private static final String A="hud_abilities.css",H="hud_health.css",C="hud_health_container.css",R="hud.css";
    private static final String[] ICONS={"haze_sleep_dagger_psd.png","haze_smoke_bomb_psd.png","haze_fixation_psd.png","haze_bullet_flurry_psd.png"};
    private static Minecraft mc(){return Minecraft.getInstance();}
    private static float screenScale(){return mc().getWindow().getGuiScaledHeight()/1080F;}
    private static float canvasWidth(){return mc().getWindow().getGuiScaledWidth()/screenScale();}
    private static int px(String sheet,String selector,String property){return(int)Math.round(HudSource.px(sheet,selector,property));}
    private static int color(String name){return HudSource.color(name);}
    private static Component component(String text,String face){return Component.literal(text).withStyle(Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("haze",face))));}
    private static void text(GuiGraphics g,String text,float x,float y,float size,String face,int color,boolean center){
        float nominal=face.equals("oracle")?32:20,factor=size/nominal;
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(factor,factor);
        Component c=component(text,face);g.drawString(mc().font,c,center?-mc().font.width(c)/2:0,0,color,false);g.pose().popMatrix();
    }
    private static void image(GuiGraphics g,String path,float x,float y,float w,float h,int tint){
        int[] size=HudSource.size(path);
        g.blit(RenderPipelines.GUI_TEXTURED,Identifier.fromNamespaceAndPath("haze","retail/"+path),Math.round(x),Math.round(y),0,0,Math.round(w),Math.round(h),size[0],size[1],size[0],size[1],tint);
    }
    public static void render(GuiGraphics g){
        ClientState s=HazeClient.own();if(!HazeClient.active||s==null)return;
        g.pose().pushMatrix();g.pose().scale(screenScale(),screenScale());
        float width=canvasWidth();
        if(!(mc().screen instanceof UpgradeScreen))abilities(g,s,width,false,-1,-1);
        health(g,s,width);
        gun(g,s,width);
        lowerLeft(g,s,width);
        channel(g,s,width);
        g.pose().popMatrix();
    }
    private static float abilityScale(){return px(R,"#hud_signature","ui-scale")/100F*px(A,"#abilities","ui-scale")/100F;}
    private static void abilities(GuiGraphics g,ClientState s,float width,boolean detail,float mouseX,float mouseY){
        float scale=abilityScale();int size=px(A,".button_container","width");
        float step=(size-10)*scale,total=(4*size-30)*scale,left=(width-total)/2,y=1080-10-size*scale;
        if(detail){g.fill((int)(left-10*scale),(int)(y-100*scale-11),(int)(left+total+10*scale),(int)(y+size*scale),color("colorCommonDarkGray"));g.fill((int)(left-10*scale),(int)(y-100*scale-11),(int)(left+total+10*scale),(int)(y-100*scale-5),color("deadlockGreen"));}
        for(int i=0;i<4;i++){
            float x=left+i*step;
            g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);
            String frame="hud/core/ability_frame_"+(i==2?"passive_":"")+s.ranks[i]+".png";
            image(g,frame,0,0,size,size,color("keybindBGColor"));
            int iconSize=(int)(size*HudSource.px(A,".ability_image","height")/100);
            image(g,ICONS[i],(size-iconSize)/2F,(size-iconSize)/2F,iconSize,iconSize,color("offBlack"));
            if(s.cooldowns[i]>0){
                int dim=(int)(size*Math.min(1,s.cooldowns[i]/Retail.DATA.prop(Retail.SIGNATURES[i],"AbilityCooldown",s.ranks[i])));
                g.enableScissor(0,0,size,dim);image(g,frame,0,0,size,size,0xb010130d);g.disableScissor();
                text(g,""+(int)Math.ceil(s.cooldowns[i]),size/2F,size/2F-11,28,"oracle",color("offWhite"),true);
            }
            text(g,""+(i+1),size/2F,size-27,px(A,".ability_key","font-size"),"radiance",color("offBlack"),true);
            if(i==1&&s.ranks[1]>=2)text(g,""+s.smokeCharges,size-23,13,18,"radiance",color("offBlack"),true);
            if(detail)upgradePips(g,s,i,size);
            g.pose().popMatrix();
            if(detail&&mouseX>=x&&mouseX<x+size*scale&&mouseY>=y-100*scale&&mouseY<y+size*scale)tooltip(g,s,i,x,y-100*scale);
        }
        image(g,"hud/ap_icon.png",width/2-27,1059,16,16,color("deadlockGreen"));
        image(g,"upgrades/infinity_psd.png",width/2-3,1050,30,30,(color("deadlockGreen")&0xffffff)|0x80000000);
    }
    private static void upgradePips(GuiGraphics g,ClientState s,int ability,int size){
        float sub=0.6F,w=170*sub,h=40*sub,space=5*sub,x=(size-w)/2F;
        for(int tier=1;tier<=3;tier++){
            float y=-(tier*(h+space));boolean owned=tier<=s.ranks[ability],next=tier==s.ranks[ability]+1;
            int bg=owned?color("deadlockGreenDarker"):next?color("deadlockGreen"):0x8010130d;
            rounded(g,x,y,w,h,h/2,bg);
            if(owned)image(g,"hud/ability_upgrade_check_psd.png",size/2F-5,y+6,10,10,0x8010130d);
            else {
                image(g,"hud/hero_ability_pip_psd.png",size/2F-18,y+6,12,12,next?color("offBlack"):0x19ff410d);
                text(g,tier==1?"1":tier==2?"2":"5",size/2F+2,y+4,13.2F,"oracle",next?color("offBlack"):0x80ff410d,false);
            }
        }
    }
    private static void health(GuiGraphics g,ClientState s,float width){
        int hcWidth=px(R,"#health_and_abilities_container","width"),hcHeight=px(R,"#health_and_abilities_container","height");
        float clampLeft=Math.max(0,(width-1920)/2);
        float left=clampLeft+300+(1920-300-1290-hcWidth)/2F;
        float top=1080-hcHeight-px(R,"#health_and_abilities_container","margin-bottom");
        g.pose().pushMatrix();g.pose().translate(left+hcWidth/2F,top+hcHeight/2F);g.pose().scale(1.2F,1.2F);g.pose().translate(-hcWidth/2F,-hcHeight/2F);
        int bw=px(H,".bars_container","width"),bh=px(H,".bars_container","height");
        g.pose().pushMatrix();g.pose().translate(hcWidth-20-bw+bw/2F,20+bh/2F);g.pose().rotate((float)Math.toRadians(-20));g.pose().translate(-bw/2F,-bh/2F);
        float x=30,y=30;int w=px(H,"#health_bar","width"),h=px(H,"#health_bar","height");
        image(g,"hud/healthbar/healthbar_backer_mask.png",x+1,y+4,w,h,0xea333333);
        double ratio=Math.max(0,Math.min(1,s.health/s.maxHealth));
        int empty=(int)(h*(1-ratio));
        g.enableScissor((int)x,(int)y+4+empty,(int)x+w+2,(int)y+h+5);
        image(g,"hud/healthbar/health_fill_masked.png",x+1,y+4,w,h,color("offWhite"));g.disableScissor();
        for(int tick=100;tick<s.maxHealth;tick+=100){int ty=(int)(y+4+h*(1-tick/s.maxHealth));g.fill((int)x+2,ty,(int)x+(tick%500==0?w-2:w/3),ty+3,0xb310130d);}
        image(g,"hud/healthbar/healthbar_frame_with_regen.png",x,y,px(H,"#health_bar_frame","width"),px(H,"#health_bar_frame","height"),color("vivaciousGreen"));
        image(g,"hud/healthbar/icon_regen_arrows.png",42,36,7,8,color("offWhite"));
        text(g,String.format(Locale.ROOT,"%.1f",Retail.DATA.stat("EBaseHealthRegen")),52,33,10,"radiance",color("offWhite"),false);
        g.pose().popMatrix();
        g.pose().pushMatrix();g.pose().translate(hcWidth/2F,127);g.pose().rotate((float)Math.toRadians(-10));g.pose().translate(-hcWidth/2F,-127);
        image(g,"hud/core/health_backer.png",145,100,90,65,color("vivaciousGreen"));
        int healthColor=s.health/s.maxHealth<0.3?0xffff5656:s.health/s.maxHealth<0.6?0xffddff56:color("offWhite");
        text(g,""+(int)Math.ceil(s.health),142,106,px(C,".currentHealthLabel","font-size"),"oracle",healthColor,false);
        text(g,"/ "+(int)s.maxHealth,181,140,px(C,".totalHealthLabel","font-size"),"radiance",0x33ffefd7,false);
        g.pose().popMatrix();g.pose().popMatrix();
    }
    private static void gun(GuiGraphics g,ClientState s,float width){
        float cx=width/2,cy=540;
        boolean aim=HazeClient.controls.down(Controls.AIM);
        double spread=(Retail.number(Retail.DATA.weapon().get("m_StandingSpread"))+s.bloom)*(aim?0.5:1);
        double gap=setting("citadel_crosshair_pip_gap")+1080*Math.tan(Math.toRadians(spread))/(2*Math.tan(Math.toRadians(HazeView.verticalFov(0))/2));
        int dot=(int)setting("citadel_crosshair_dot_size"),outline=(int)setting("citadel_crosshair_dot_outline_border");
        rounded(g,cx-dot/2F-outline,cy-dot/2F-outline,dot+2*outline,dot+2*outline,(dot+2*outline)/2F,opacity(0xff000000,setting("citadel_crosshair_dot_outline_opacity")));
        rounded(g,cx-dot/2F,cy-dot/2F,dot,dot,dot/2F,opacity(0xffffffff,setting("citadel_crosshair_dot_opacity")));
        if(s.reload<=0&&!s.flurry)for(int direction=0;direction<4;direction++){
            g.pose().pushMatrix();g.pose().translate(cx,cy);g.pose().rotate((float)(direction*Math.PI/2));
            int half=(int)setting("citadel_crosshair_pip_width")/2,len=(int)setting("citadel_crosshair_pip_height"),offset=(int)gap;
            if(aim){g.fill(-half-1,-offset-len-1,half+1,-offset+1,opacity(0xff000000,setting("citadel_crosshair_pip_outline_opacity")));g.fill(-half,-offset-len,half,-offset,opacity(0xffffffff,setting("citadel_crosshair_pip_opacity")));}
            else {g.fill(-8,-offset-2,8,-offset+2,0xff000000);g.fill(-7,-offset-1,7,-offset+1,0xffffffff);}
            g.pose().popMatrix();
        }
        if(setting("citadel_crosshair_enable_clip_status")>0)arc(g,cx,cy,47,4,45,135,s.ammo/(double)Retail.DATA.weapon().get("m_iClipSize").getAsInt(),s.ammo<5?color("colorEnemy"):0xccffffff);
        if(s.flurry)image(g,"icons/icon_infinite.png",cx-12.5F,cy+80,25,25,color("offWhite"));
        else {text(g,""+s.ammo,cx-20,cy+80,16,"radiance",0xccffffff,true);text(g," / "+Retail.DATA.weapon().get("m_iClipSize").getAsInt(),cx-8,cy+80,16,"radiance",0x1affffff,false);}
        Motion motion=HazeClient.motion;
        if(motion!=null)for(var charge:StaminaDisplay.charges(motion.stamina,(int)motion.maxStamina())) {
            // Panorama starts radial angles at the top; Java's arc helper starts at the right.
            double start=charge.start()-90,end=start+charge.extent();
            arc(g,cx,cy,59.5F,4,start,end,1,0x66000000);
            arc(g,cx,cy,58.5F,8,start,end,charge.fill(),charge.fill()<1?0x66ffee00:0x66ffffff);
        }
        if(s.reload>0){arc(g,cx,cy,50,4,0,360,1,0x59242424);arc(g,cx,cy,50,4,45,135,1-s.reload/Retail.DATA.weapon().get("m_reloadDuration").getAsDouble(),color("colorEnemy"));}
    }
    private static double setting(String name){var local=Retail.DATA.root.getAsJsonObject("localInputSettings");return local.has(name)?Retail.number(local.get(name)):Retail.DATA.nativeFloat(name);}
    private static int opacity(int rgb,double alpha){return(rgb&0xffffff)|((int)Math.round(alpha*255)<<24);}
    private static void rounded(GuiGraphics g,float x,float y,float w,float h,float radius,int color){
        for(int row=0;row<Math.ceil(h);row++){double dy=Math.max(0,Math.abs(row+0.5-h/2)-(h/2-radius));double inset=radius-Math.sqrt(Math.max(0,radius*radius-dy*dy));g.fill(Math.round(x+(float)inset),Math.round(y+row),Math.round(x+w-(float)inset),Math.round(y+row+1),color);}
    }
    private static void lowerLeft(GuiGraphics g,ClientState s,float width){
        float left=Math.max(0,(width-1920)/2);
        // Unpurchased universal inventory, following the current retail two-row panel.
        float scale=1.2F,x=left+22,y=1080-16-102*scale;
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(scale,scale);
        int base=HudSource.DATA.getAsJsonObject("nativeUi").get("baseItemSlots").getAsInt(),flex=HudSource.DATA.getAsJsonObject("nativeUi").get("lockedItemSlots").getAsInt();
        for(int col=0;col<(base+flex)/2;col++)for(int row=0;row<2;row++){
            rounded(g,col*51+3,row*51+3,45,45,3,0x04ffffff);
            if(col>=base/2)image(g,"hud/shop_ability_upgrades_lock_icon_png.png",col*51+16.5F,row*51+14.25F,18,22.5F,0x26ffffff);
        }
        g.pose().popMatrix();
        image(g,"hud/core/spirit_jar_frame_png.png",left+30,875,60,60,color("offWhite"));
        text(g,"1",left+62.5F,896,28,"mono",color("offWhite"),true);
        image(g,"hud/icons/icon_soul.png",left+88,889,20,36,color("shardColor"));
        text(g,"0",left+114,889,32,"oracle",color("blueAPTextColor"),false);
        text(g,HudSource.loc("Citadel_Hero_Stats_Souls").toUpperCase(Locale.ROOT),left+145,909,14,"radiance",0x66ffefd7,false);
    }
    private static void channel(GuiGraphics g,ClientState s,float width){
        if(!s.flurry&&!s.smoke)return;
        boolean smoke=!s.flurry;int index=smoke?1:3;
        double remaining=smoke?s.smokeRemaining:s.danceRemaining,duration=Retail.DATA.prop(Retail.SIGNATURES[index],"AbilityDuration",s.ranks[index]);
        String label=HudSource.loc(Retail.SIGNATURES[index]).toUpperCase(Locale.ROOT);
        float scale=smoke?0.7F:1,cx=width/2+(smoke?0:250),cy=1080-190;
        g.pose().pushMatrix();g.pose().translate(cx,cy);g.pose().scale(scale,scale);g.pose().rotate(smoke?0:(float)Math.toRadians(-10));
        float actualWidth=mc().font.width(component(label,"oracle"))*1.5F;
        text(g,label,0,-64,48,"oracle",0x66ffefd7,true);
        g.enableScissor((int)(-actualWidth/2)-16,-80,(int)(-actualWidth/2+actualWidth*Math.max(0,Math.min(1,remaining/duration))),0);
        text(g,label,0,-64,48,"oracle",color("hazeColor"),true);g.disableScissor();
        g.pose().popMatrix();
    }
    private static void arc(GuiGraphics g,float cx,float cy,float radius,int thickness,double start,double end,double fraction,int tint){
        int steps=(int)Math.ceil(Math.abs(end-start)*Math.max(0,Math.min(1,fraction)));
        for(int i=0;i<steps;i++){
            double angle=Math.toRadians(start+Math.signum(end-start)*i);float x=cx+(float)Math.cos(angle)*radius,y=cy+(float)Math.sin(angle)*radius;
            g.pose().pushMatrix();g.pose().translate(x,y);g.pose().rotate((float)angle);g.fill(-thickness/2,-1,thickness/2+1,2,tint);g.pose().popMatrix();
        }
    }
    public static void upgrades(GuiGraphics g,int mouseX,int mouseY){
        ClientState s=HazeClient.own();if(s==null)return;
        g.pose().pushMatrix();g.pose().scale(screenScale(),screenScale());abilities(g,s,canvasWidth(),true,mouseX/screenScale(),mouseY/screenScale());g.pose().popMatrix();
    }
    public static int upgradeAt(double mouseX,double mouseY){
        float scale=abilityScale();int size=px(A,".button_container","width");float total=(4*size-30)*scale,left=(canvasWidth()-total)/2,y=1080-10-size*scale;
        double x=mouseX/screenScale(),my=mouseY/screenScale();
        ClientState s=HazeClient.own();if(s==null)return -1;
        for(int i=0;i<4;i++){float ix=left+i*(size-10)*scale,py=y-(s.ranks[i]+1)*27*scale;if(s.ranks[i]<3&&x>=ix+9*scale&&x<ix+111*scale&&my>=py&&my<py+24*scale)return i;}return -1;
    }
    private static void tooltip(GuiGraphics g,ClientState s,int ability,float x,float bottom){
        String id=Retail.SIGNATURES[ability],description=interpolate(HudSource.loc(id+"_desc"),ability,s.ranks[ability],false);
        float w=540,pad=40;int lines=mc().font.split(component(description.replaceAll("<[^>]*>",""),"radiance"),(int)((w-pad*2)/0.9F)).size();
        float bodyHeight=100+lines*23.4F+95,y=bottom-bodyHeight-170,left=Math.max(12,x-w-12);
        image(g,"tooltips/ability/ability_tooltip_bg_png.png",left,y,w,bodyHeight,color("offWhite"));
        text(g,HudSource.loc(id),left+36,y+30,36,"oracle",color("offWhite"),false);
        text(g,format(Retail.DATA.prop(id,"AbilityCooldown",s.ranks[ability]))+"s",left+w-55,y+34,24,"radiance",color("offWhite"),true);
        paragraph(g,description,left+pad,y+94,w-2*pad,18,color("silvered"),false);
        var sections=Retail.DATA.ability(id).getAsJsonObject("m_AbilityTooltipDetails").getAsJsonArray("m_vecAbilityInfoSections");
        List<String> properties=new ArrayList<>();
        for(var section:sections)if(section.getAsJsonObject().has("m_vecAbilityPropertiesBlock"))for(var block:section.getAsJsonObject().getAsJsonArray("m_vecAbilityPropertiesBlock"))for(var p:block.getAsJsonObject().getAsJsonArray("m_vecAbilityProperties")){
            String property=p.getAsJsonObject().get("m_strImportantProperty").getAsString();if(Retail.DATA.ability(id).getAsJsonObject("m_mapAbilityProperties").has(property))properties.add(property);
        }
        for(int p=0;p<properties.size();p++){
            String key=properties.get(p);float cx=left+pad+(w-2*pad)/Math.max(1,properties.size())*(p+0.5F);
            text(g,format(Retail.DATA.prop(id,key,s.ranks[ability]))+HudSource.loc(key+"_postfix").replace(key+"_postfix",""),cx,y+bodyHeight-74,30,"oracle",color("offWhite"),true);
            text(g,HudSource.loc(key+"_label"),cx,y+bodyHeight-36,14,"radiance",0x99ffefd7,true);
        }
        for(int tier=1;tier<=3;tier++){
            float cw=(w-28)/3,cx=left+14+(tier-1)*cw,cy=y+bodyHeight;boolean owned=tier<=s.ranks[ability],available=tier==s.ranks[ability]+1;
            image(g,"tooltips/ability/"+(available?"ap_rect_available_png.png":"ap_rect_default_png.png"),cx,cy,cw-6,130,color("offWhite"));
            paragraph(g,interpolate(HudSource.loc(id+"_t"+tier+"_desc"),ability,tier,true),cx+8,cy+16,cw-22,16,available?color("offBlack"):0x99ffefd7,true);
            g.fill((int)cx,(int)cy+130,(int)(cx+cw-6),(int)cy+160,owned?color("deadlockGreenDarker"):color("offBlack"));
            if(owned)image(g,"hud/ability_upgrade_check_psd.png",cx+cw/2-10,cy+135,20,20,color("deadlockGreen"));
            else {image(g,"hud/ap_icon.png",cx+cw/2-20,cy+137,16,16,color("deadlockGreen"));text(g,tier==1?"1":tier==2?"2":"5",cx+cw/2+6,cy+133,20,"radiance",color("offWhite"),true);}
        }
    }
    private static String format(double value){return String.format(Locale.ROOT,"%.2f",value).replaceAll("\\.?0+$","");}
    private static String interpolate(String text,int ability,int rank,boolean delta){
        java.util.regex.Matcher tokens=java.util.regex.Pattern.compile("\\{[sfdi]:([^}]+)}").matcher(text);StringBuffer out=new StringBuffer();
        while(tokens.find()){
            String key=tokens.group(1);var props=Retail.DATA.ability(Retail.SIGNATURES[ability]).getAsJsonObject("m_mapAbilityProperties");String value=tokens.group();
            if(props.has(key)){double n=Retail.DATA.prop(Retail.SIGNATURES[ability],key,rank);if(delta)n-=Retail.DATA.prop(Retail.SIGNATURES[ability],key,rank-1);value=format(n);}
            tokens.appendReplacement(out,java.util.regex.Matcher.quoteReplacement(value));
        }tokens.appendTail(out);return out.toString().replaceAll("<br\\s*/?>","\n");
    }
    private static void paragraph(GuiGraphics g,String html,float x,float y,float width,float size,int tint,boolean center){
        MutableComponent content=Component.empty();String[] spans=html.split("(?=<span)|(?<=</span>)");
        for(String span:spans){boolean highlighted=span.contains("class=\"highlight");String raw=span.replaceAll("<[^>]*>","");content.append(Component.literal(raw).withStyle(style->style.withColor(highlighted?tint==color("offBlack")?tint:color("offWhite"):tint)));}
        content.withStyle(Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("haze","radiance"))));
        float factor=size/20;var lines=mc().font.split(content,(int)(width/factor));
        g.pose().pushMatrix();g.pose().translate(x,y);g.pose().scale(factor,factor);int line=0;
        for(var text:lines){int dx=center?Math.max(0,((int)(width/factor)-mc().font.width(text))/2):0;g.drawString(mc().font,text,dx,Math.round(line*26),tint,false);line++;}
        g.pose().popMatrix();
    }
}
