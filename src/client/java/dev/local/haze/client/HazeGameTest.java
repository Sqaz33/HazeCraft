package dev.local.haze.client;

import dev.local.haze.*;
import dev.local.haze.sim.*;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.golem.IronGolem;
import java.nio.file.*;
import com.google.gson.*;

/** Real client oracle, entirely internal controls; no operating-system mouse/keyboard driving. */
public final class HazeGameTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if(!Boolean.getBoolean("haze.lab"))return;
        JsonObject report=new JsonObject();
        try(var world=context.worldBuilder().create()) {
            var server=world.getServer();
            server.runCommand("fill -24 63 -24 24 63 48 smooth_stone");
            server.runCommand("tp @p 0 64 0 0 0");
            server.runCommand("gamemode survival @p");
            server.runOnServer(s->{
                var p=s.getPlayerList().getPlayers().getFirst();
                HazeActor actor=HazeMod.ACTORS.computeIfAbsent(p.getUUID(),HazeActor::new);actor.toggle(p);
                IronGolem target=EntityType.IRON_GOLEM.create(p.level(),EntitySpawnReason.COMMAND);
                if(target==null)throw new AssertionError("Target spawn failed");
                target.setPos(0,64,12);target.setNoAi(true);target.setPersistenceRequired();
                target.getAttribute(Attributes.MAX_HEALTH).setBaseValue(10000);target.setHealth(10000);p.level().addFreshEntity(target);
                report.addProperty("targetId",target.getId());
            });
            context.waitFor(mc->HazeClient.active&&HazeClient.own()!=null,300);
            context.runOnClient(mc->{mc.setScreen(null);mc.player.setYRot(0);mc.player.setXRot(0);});
            world.getClientWorld().waitForChunksRender();context.waitTicks(20);
            require(context.computeOnClient(mc->HazeRenderer.renderedFrames>0),"Haze geometry never rendered");
            context.takeScreenshot("haze-01-idle");report.addProperty("idleRendered",true);
            context.runOnClient(mc->{
                var mouse=(dev.local.haze.client.mixin.MouseAccessor)mc.mouseHandler;
                float before=mc.player.getYRot();mouse.haze$dx(100);mouse.haze$dy(0);mouse.haze$turn(1.0/60);mouse.haze$dx(0);
                double expected=Retail.number(Retail.DATA.root.getAsJsonObject("localInputSettings").get("sensitivity"))*Retail.number(Retail.DATA.root.getAsJsonObject("localInputSettings").get("m_yaw"))*100;
                require(Math.abs(mc.player.getYRot()-before-expected)<1e-5,"Source mouse counts differ");mc.player.setYRot(0);report.addProperty("sourceMouseDegreesPer100Counts",expected);
            });
            context.runOnClient(mc->mc.setScreen(new UpgradeScreen()));context.takeScreenshot("haze-01b-upgrades-available");context.setScreen(()->null);
            context.runOnClient(mc->HazeLab.held=Controls.FIRE);context.waitTicks(25);
            context.runOnClient(mc->HazeLab.held=0);context.waitTicks(2);
            boolean stacks=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();return HazeMod.ACTORS.get(p.getUUID()).targets.values().stream().anyMatch(f->f.stacks>0);});
            require(stacks,"Bullets never added Fixation to target");report.addProperty("fixationOnMob",true);context.takeScreenshot("haze-02-fire-stacks");
            context.runOnClient(mc->HazeLab.held=Controls.DAGGER);context.waitTicks(1);context.runOnClient(mc->HazeLab.held=0);context.waitTicks(12);
            boolean sleeping=server.computeOnServer(s->{var p=s.getPlayerList().getPlayers().getFirst();var a=HazeMod.ACTORS.get(p.getUUID());return a.targets.values().stream().anyMatch(f->f.asleep(a.abilities.time));});
            require(sleeping,"Sleep Dagger did not put target to sleep");report.addProperty("sleepDagger",true);context.takeScreenshot("haze-03-dagger-sleep");
            context.runOnClient(mc->{for(int i=0;i<4;i++)for(int rank=0;rank<3;rank++)HazeClient.send(10+i);});context.waitTicks(4);
            context.runOnClient(mc->mc.setScreen(new UpgradeScreen()));context.takeScreenshot("haze-04-upgrades");context.setScreen(()->null);
            context.runOnClient(mc->HazeLab.held=Controls.SMOKE);context.waitTicks(1);context.runOnClient(mc->HazeLab.held=0);context.waitTicks(36);
            require(context.computeOnClient(mc->HazeClient.own().invisible),"Smoke invisibility did not activate");report.addProperty("smoke",true);context.takeScreenshot("haze-05-smoke");
            context.runOnClient(mc->HazeLab.held=Controls.FLURRY);context.waitTicks(1);context.runOnClient(mc->HazeLab.held=0);context.waitTicks(15);
            require(context.computeOnClient(mc->HazeClient.own().flurry),"Bullet Dance did not activate");report.addProperty("bulletDance",true);context.takeScreenshot("haze-06-dance");
            context.waitTicks(80);
            double start=context.computeOnClient(mc->mc.player.getZ());
            context.runOnClient(mc->HazeLab.held=Controls.FORWARD|Controls.DASH);context.waitTicks(1);
            context.runOnClient(mc->HazeLab.held=Controls.FORWARD);context.waitTicks(13);
            context.runOnClient(mc->HazeLab.held=0);
            double moved=context.computeOnClient(mc->mc.player.getZ())-start;require(moved>8,"Dash displacement too small: "+moved);report.addProperty("dashMeters",moved);context.takeScreenshot("haze-07-dash");
            context.runOnClient(mc->HazeLab.held=Controls.JUMP);context.waitTicks(1);context.runOnClient(mc->HazeLab.held=0);context.waitTicks(3);
            require(context.computeOnClient(mc->!mc.player.onGround()),"Jump failed");context.takeScreenshot("haze-08-jump");report.addProperty("jump",true);
            context.runOnClient(mc->HazeClient.send(1));context.waitTicks(3);
            require(context.computeOnClient(mc->!HazeClient.active),"Steve toggle failed");context.takeScreenshot("haze-09-steve");report.addProperty("toggleBack",true);
        } finally {
            HazeLab.held=0;
            try{Files.createDirectories(Path.of("D:/DeadlockHazeLab/evidence"));Files.writeString(Path.of("D:/DeadlockHazeLab/evidence/client-report.json"),new GsonBuilder().setPrettyPrinting().create().toJson(report));}catch(Exception e){throw new RuntimeException(e);}
        }
        HazeMod.LOG.info("HAZE_LAB_PASS {}",report);
    }
    private static void require(boolean condition,String message){if(!condition)throw new AssertionError(message);}
}
