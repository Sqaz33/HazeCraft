package dev.local.haze;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class HazeMod implements ModInitializer {
    public static final Logger LOG=LoggerFactory.getLogger("haze");
    public static final Map<UUID,HazeActor> ACTORS=new HashMap<>();
    @Override public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(HazeProtocol.Input.TYPE,HazeProtocol.Input.CODEC);
        PayloadTypeRegistry.playC2S().register(HazeProtocol.Debug.TYPE,HazeProtocol.Debug.CODEC);
        PayloadTypeRegistry.playS2C().register(HazeProtocol.Snapshot.TYPE,HazeProtocol.Snapshot.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(HazeProtocol.Input.TYPE,(packet,context)->{
            ServerPlayer p=context.player();ACTORS.computeIfAbsent(p.getUUID(),HazeActor::new).accept(packet,p);
        });
        ServerPlayNetworking.registerGlobalReceiver(HazeProtocol.Debug.TYPE,(packet,context)->{
            ServerPlayer p=context.player();HazeActor actor=ACTORS.get(p.getUUID());
            if(actor!=null)actor.debug(packet,p);
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler,server)->{HazeActor a=ACTORS.remove(handler.player.getUUID());if(a!=null&&a.active)a.toggle(handler.player);});
        ServerTickEvents.END_SERVER_TICK.register(server->{
            for(ServerPlayer p:server.getPlayerList().getPlayers()) {
                HazeActor a=ACTORS.get(p.getUUID());if(a==null) continue;
                a.tick(p);
                HazeProtocol.Snapshot packet=new HazeProtocol.Snapshot(a.snapshot(p));
                for(ServerPlayer viewer:server.getPlayerList().getPlayers()) if(ServerPlayNetworking.canSend(viewer,HazeProtocol.Snapshot.TYPE)) ServerPlayNetworking.send(viewer,packet);
            }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity,source,amount)->{
            HazeActor a=ACTORS.get(entity.getUUID());
            if(a!=null && a.parrying() && source.getEntity() instanceof Mob mob && source.getDirectEntity()==mob) { a.parry(mob);return false; }
            if(a!=null&&a.active&&a.abilities.flurry()&&source.is(net.minecraft.tags.DamageTypeTags.IS_PROJECTILE)
                &&entity.getRandom().nextDouble()*100<a.abilities.p(3,"EvasionPercent"))return false;
            return true;
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,base,taken,blocked)->{
            if(taken<=0)return;
            HazeActor damaged=ACTORS.get(entity.getUUID());if(damaged!=null&&damaged.active)damaged.combat(source.getEntity());
            for(HazeActor actor:ACTORS.values())if(actor.active&&actor.dimension==entity.level().dimension()){
                var f=actor.targets.get(entity.getId());if(f!=null)f.damaged(dev.local.haze.sim.Retail.DATA,actor.abilities.ranks[0],actor.abilities.time);
            }
        });
        LOG.info("Haze local port, source Deadlock build {}, protocol {}",dev.local.haze.sim.Retail.DATA.root.get("build").getAsString(),HazeProtocol.VERSION);
    }
}
