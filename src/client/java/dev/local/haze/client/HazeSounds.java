package dev.local.haze.client;

import com.google.gson.*;
import dev.local.haze.sim.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.*;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.*;
import net.minecraft.world.entity.Entity;
import java.util.*;
import java.io.*;
import java.nio.charset.StandardCharsets;

public final class HazeSounds {
    private static final JsonObject EVENTS=load();
    private static final Map<UUID,Loops> LOOPS=new HashMap<>();
    private static boolean aimed;
    private record AnimationClock(String clip,double time){}
    private static final Map<String,AnimationClock> ANIMATION_CLOCKS=new HashMap<>();
    private static JsonObject load(){try(var in=HazeSounds.class.getResourceAsStream("/haze/retail/sound-events.json")){return JsonParser.parseReader(new InputStreamReader(in,StandardCharsets.UTF_8)).getAsJsonObject();}catch(IOException e){throw new UncheckedIOException(e);}}
    private static final class Loops{SoundInstance smoke,dance;}
    private static SoundInstance sound(String event,Entity e,boolean loop){
        String key=event.toLowerCase(Locale.ROOT);if(!EVENTS.has(key))return null;
        return new SimpleSoundInstance(Identifier.fromNamespaceAndPath("haze",key),SoundSource.PLAYERS,1,1,SoundInstance.createUnseededRandom(),loop,0,
            SoundInstance.Attenuation.LINEAR,e.getX(),e.getY()+1,e.getZ(),false);
    }
    private static void play(String name,Entity entity){SoundInstance sound=sound(name,entity,false);if(sound!=null)Minecraft.getInstance().getSoundManager().play(sound);}
    public static void received(ClientState old,ClientState state){
        Minecraft mc=Minecraft.getInstance();if(mc.level==null)return;Entity e=mc.level.getEntity(state.entityId);if(e==null)return;
        if(!state.active){stop(state.uuid);return;}if(old==null||!old.active)return;
        if(state.shots>old.shots)for(long i=old.shots;i<state.shots;i++)play(i==0||old.time-Math.max(old.leftShot,old.rightShot)>.3?"Haze.Wpn.Fire.First":"Haze.Wpn.Fire.Main",e);
        if(state.shots==old.shots&&old.time-Math.max(old.leftShot,old.rightShot)<0.05&&state.time-Math.max(state.leftShot,state.rightShot)>=0.05)play("Haze.Wpn.Fire.Release",e);
        if(old.reload<=0&&state.reload>0)play("Haze.Wpn.Reload.Start",e);
        if(state.action.equals("dagger")&&state.actionTime<old.actionTime)play("Haze.Finesse.Dagger.Cast",e);
        if(state.smoke&&!old.smoke)play("Haze.Smoke.Bomb.Cast",e);
        if(old.smoke&&!state.smoke)play("Haze.Smoke.Bomb.End",e);
        if(state.action.equals("flurry")&&state.actionTime<old.actionTime)play("Haze.BulletFlurry.Cast.Delay",e);
        if(state.flurry&&!old.flurry)play("Haze.BulletFlurry.Modifier.Start",e);
        if(old.flurry&&!state.flurry)play("Haze.BulletFlurry.Modifier.End",e);
        for(ClientState.Stack stack:state.stacks){boolean slept=old.stacks.stream().anyMatch(x->x.id()==stack.id()&&x.sleep());Entity target=mc.level.getEntity(stack.id());if(target!=null&&stack.sleep()&&!slept)play("Haze.Finesse.Dagger.Projectile.Hit",target);if(target!=null&&!stack.sleep()&&slept)play("Haze.Finesse.Dagger.Wake",target);}
        Loops loops=LOOPS.computeIfAbsent(state.uuid,k->new Loops());
        if(state.smoke&&loops.smoke==null){loops.smoke=sound("Haze.Smoke.Bomb.Modifier.Lp",e,true);if(loops.smoke!=null)mc.getSoundManager().play(loops.smoke);}
        if(!state.smoke&&loops.smoke!=null){mc.getSoundManager().stop(loops.smoke);loops.smoke=null;}
        if(state.flurry&&loops.dance==null){loops.dance=sound("Haze.BulletFlurry.Modifier.Lp",e,true);if(loops.dance!=null)mc.getSoundManager().play(loops.dance);}
        if(!state.flurry&&loops.dance!=null){mc.getSoundManager().stop(loops.dance);loops.dance=null;}
    }
    public static void animation(UUID id,Entity entity,String clip,double time,boolean loop){
        animation(id,entity,"base",clip,time,loop);
    }
    public static void animation(UUID id,Entity entity,String channel,String clip,double time,boolean loop){
        String clockKey=id+"/"+channel;
        AnimationClock previous=ANIMATION_CLOCKS.put(clockKey,new AnimationClock(clip,time));
        double before=previous!=null&&previous.clip.equals(clip)?previous.time:time;
        if(time<=before)return;JsonObject data=ClipMetadata.DATA.getAsJsonObject(clip);if(data==null)return;
        double duration=data.get("duration").getAsDouble();if(duration<=0)return;
        int first=loop?(int)Math.floor(before/duration):0,last=loop?(int)Math.floor(time/duration):0;
        for(var e:data.getAsJsonArray("events")){
            var event=e.getAsJsonObject();if(!event.has("sound")||event.get("sound").getAsString().isEmpty())continue;
            double at=event.get("frame").getAsDouble()/Math.max(1,data.get("frames").getAsInt()-1)*duration;
            for(int cycle=first;cycle<=Math.min(last,first+1);cycle++)if(before<at+cycle*duration&&time>=at+cycle*duration)play(event.get("sound").getAsString(),entity);
        }
    }
    public static void movement(Motion.Mode before,long jumpBefore,Motion m,Controls controls,Entity e){
        if(jumpBefore!=m.jumpSerial){
            String key=switch(m.jumpKind){case GROUND->"m_GroundJumpExecutedSound";case AIR->"m_AirJumpSound";case WALL->"m_WallJumpExecutedSound";case DASH->"m_strDashJumpSound";};
            play(Retail.DATA.ability("citadel_ability_jump").get(key).getAsString(),e);
        }
        if(before!=m.mode){String event=switch(m.mode){case DASH_GROUND->"Player.Dash.Ground";case DASH_AIR->"Player.Dash.Air";case MANTLE->"Haze.Mantle";case AIR->"";default->before==Motion.Mode.AIR?"Haze.JumpLand":"";};if(!event.isEmpty())play(event,e);}
        if(controls.edge(Controls.PARRY))play("Player.Melee.Parry.Shared",e);
        if(controls.down(Controls.AIM)!=aimed){aimed=controls.down(Controls.AIM);play(aimed?"Haze.Wpn.ZoomIn":"Haze.Wpn.ZoomOut",e);}
    }
    private static void stop(UUID id){Loops loops=LOOPS.remove(id);if(loops==null)return;var manager=Minecraft.getInstance().getSoundManager();if(loops.smoke!=null)manager.stop(loops.smoke);if(loops.dance!=null)manager.stop(loops.dance);}
    public static void clear(){for(UUID id:new ArrayList<>(LOOPS.keySet()))stop(id);ANIMATION_CLOCKS.clear();aimed=false;}
}
