package dev.local.haze.client;

import com.google.gson.JsonParser;
import dev.local.haze.*;
import dev.local.haze.sim.*;
import dev.local.haze.sim.movement.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.*;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import java.util.*;
import org.lwjgl.glfw.GLFW;

public final class HazeClient implements ClientModInitializer,HazeHooks.ClientMotion {
    public static final Map<UUID,ClientState> STATES=new HashMap<>();
    public static Motion motion;
    public static Controls controls=Controls.NONE;
    public static boolean active;
    private static int sequence,previous,previousUpgradeKeys,previousItemKeys;
    private static boolean toggleWasDown;
    private static long lastItemMovement,lastItemStamina;
    private static CameraType previousCamera;
    private static MovementClock movementClock=new MovementClock();
    private static MovementKeys movementKeys=new MovementKeys();
    private static PlayerCollision playerCollision;
    private static void clearInput() { controls=Controls.NONE;previous=0;movementKeys=new MovementKeys();movementClock.clearInputs(); }
    public static boolean debugKey(long window,int key,int action) {
        Minecraft mc=Minecraft.getInstance();
        if(key!=GLFW.GLFW_KEY_F8||window!=mc.getWindow().handle()||!mc.isWindowActive()||!DebugScreen.allowed()
            ||(mc.screen!=null&&!(mc.screen instanceof DebugScreen)))return false;
        if(action==GLFW.GLFW_PRESS){clearInput();send(0);mc.setScreen(mc.screen instanceof DebugScreen?null:new DebugScreen());}
        return true;
    }
    public static void debugResetMovement() {
        if(!DebugScreen.allowed()||!active)return;
        clearInput();motion=new Motion(Retail.DATA,10);motion.grounded=Minecraft.getInstance().player.onGround();
        movementClock=new MovementClock();playerCollision=null;
    }
    public static void keyboard(int key,int action) {
        Minecraft mc=Minecraft.getInstance();
        if(!active||!mc.isWindowActive()||mc.screen!=null||action==GLFW.GLFW_REPEAT)return;
        int bit=switch(key) {
            case GLFW.GLFW_KEY_W->Controls.FORWARD;case GLFW.GLFW_KEY_S->Controls.BACK;
            case GLFW.GLFW_KEY_A->Controls.LEFT;case GLFW.GLFW_KEY_D->Controls.RIGHT;
            case GLFW.GLFW_KEY_SPACE->Controls.JUMP;case GLFW.GLFW_KEY_LEFT_SHIFT->Controls.DASH;
            case GLFW.GLFW_KEY_LEFT_CONTROL->Controls.CROUCH;case GLFW.GLFW_KEY_F->Controls.PARRY;
            case GLFW.GLFW_KEY_Q->Controls.MELEE;default->0;
        };
        if(key==GLFW.GLFW_KEY_I&&action==GLFW.GLFW_PRESS){clearInput();send(0);mc.setScreen(new CharacterStatsScreen(null));return;}
        if(bit!=0)movementKeys.event(bit,action==GLFW.GLFW_PRESS);
    }
    public static boolean shopKey(long window,int key,int action){
        var mc=Minecraft.getInstance();
        if(!active||key!=GLFW.GLFW_KEY_B||window!=mc.getWindow().handle()||!mc.isWindowActive()||(mc.screen!=null&&!(mc.screen instanceof ShopScreen)))return false;
        if(action==GLFW.GLFW_PRESS){clearInput();send(0);mc.setScreen(mc.screen instanceof ShopScreen?null:new ShopScreen());}return true;
    }
    public static void shop(String item,boolean sell){
        if(active&&ClientPlayNetworking.canSend(HazeProtocol.Shop.TYPE))ClientPlayNetworking.send(new HazeProtocol.Shop(item,sell));
    }
    public static boolean ownsMeleeKey(long window,int key){
        var mc=Minecraft.getInstance();
        return active&&key==GLFW.GLFW_KEY_Q&&window==mc.getWindow().handle()&&mc.isWindowActive()&&mc.screen==null;
    }
    public static ClientState own() { Minecraft mc=Minecraft.getInstance();return mc.player==null?null:STATES.get(mc.player.getUUID()); }
    public static boolean active(UUID id) { ClientState state=STATES.get(id);return state!=null&&state.active; }
    @Override public void onInitializeClient() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents.CLIENT_STOPPING.register(mc->HazeRenderer.closeMeshes());
        HazeHooks.client=this;
        ClientPlayNetworking.registerGlobalReceiver(HazeProtocol.Snapshot.TYPE,(packet,context)->{
            ClientState state=new ClientState(JsonParser.parseString(packet.json()).getAsJsonObject());ClientState old=STATES.put(state.uuid,state);HazeSounds.received(old,state);
            Minecraft mc=context.client();
            if(mc.player!=null && mc.player.getUUID().equals(state.uuid)) {
                if(motion!=null&&old!=null&&state.meleeStrikeSerial!=old.meleeStrikeSerial){
                    if(motion.melee.dashing())motion.melee.strike();motion.melee.syncCooldown(state.meleeCooldown);
                }
                if(state.active!=active) {
                    active=state.active;
                    if(active) {
                        motion=new Motion(Retail.DATA,10);movementClock=new MovementClock();clearInput();HazeView.reset();previousCamera=mc.options.getCameraType();mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
                    } else {
                        if(previousCamera!=null)mc.options.setCameraType(previousCamera);motion=null;clearInput();
                        if(mc.screen instanceof UpgradeScreen)mc.setScreen(null);
                    }
                    mc.player.refreshDimensions();
                }
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler,mc)->{
            DebugTools.clear();
            if(previousCamera!=null&&active)mc.options.setCameraType(previousCamera);
            active=false;motion=null;STATES.clear();clearInput();sequence=0;movementClock=new MovementClock();
            HazeSounds.clear();
        });
        ClientTickEvents.START_CLIENT_TICK.register(HazeClient::tick);
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("haze","hud"),(g,dt)->HazeHud.render(g));
        HudElementRegistry.addLast(Identifier.fromNamespaceAndPath("haze","debug_overlay"),(g,dt)->DebugTools.renderOverlay(g));
        WorldRenderEvents.END_EXTRACTION.register(HazeRenderer::extract);
        WorldRenderEvents.AFTER_ENTITIES.register(HazeRenderer::draw);
        WorldRenderEvents.AFTER_ENTITIES.register(HazeEffects::draw);
        HazeMod.LOG.info("Haze client ready: F6 toggle, retail default controls, hold Alt for upgrades");
    }
    private static boolean key(long handle,int key) { return GLFW.glfwGetKey(handle,key)==GLFW.GLFW_PRESS; }
    private static boolean button(long handle,int key) { return GLFW.glfwGetMouseButton(handle,key)==GLFW.GLFW_PRESS; }
    private static void tick(Minecraft mc) {
        DebugTools.permissions();
        if(mc.player==null||mc.level==null) { clearInput();return; }
        long window=mc.getWindow().handle();
        boolean toggle=key(window,GLFW.GLFW_KEY_F6);
        if(toggle&&!toggleWasDown&&mc.screen==null) send(1);
        toggleWasDown=toggle;
        if(!active) return;
        mc.options.setCameraType(CameraType.THIRD_PERSON_BACK);
        if(!mc.isWindowActive()&&!Boolean.getBoolean("haze.lab")) { clearInput();send(0);return; }
        boolean alt=key(window,GLFW.GLFW_KEY_LEFT_ALT)||key(window,GLFW.GLFW_KEY_RIGHT_ALT);
        if(alt&&mc.screen==null) mc.setScreen(new UpgradeScreen());
        if(!alt&&mc.screen instanceof UpgradeScreen&&!Boolean.getBoolean("haze.lab")) mc.setScreen(null);
        int upgrades=0;
        if(alt)for(int i=0;i<4;i++)if(key(window,GLFW.GLFW_KEY_1+i))upgrades|=1<<i;
        for(int i=0;i<4;i++)if((upgrades&(1<<i))!=0&&(previousUpgradeKeys&(1<<i))==0)send(10+i);
        previousUpgradeKeys=upgrades;
        int itemKeys=0;int[] activeKeys={GLFW.GLFW_KEY_Z,GLFW.GLFW_KEY_X,GLFW.GLFW_KEY_C,GLFW.GLFW_KEY_V};
        if(mc.screen==null&&!alt)for(int i=0;i<4;i++)if(key(window,activeKeys[i]))itemKeys|=1<<i;
        for(int i=0;i<4;i++)if((itemKeys&(1<<i))!=0&&(previousItemKeys&(1<<i))==0)send(200+i,0);
        previousItemKeys=itemKeys;
        if(mc.screen!=null&&!(mc.screen instanceof UpgradeScreen)&&!(mc.screen instanceof ShopScreen)) { clearInput();send(0);return; }
        int held=0;
        int[] keys={GLFW.GLFW_KEY_W,GLFW.GLFW_KEY_S,GLFW.GLFW_KEY_A,GLFW.GLFW_KEY_D,GLFW.GLFW_KEY_SPACE,GLFW.GLFW_KEY_LEFT_SHIFT,GLFW.GLFW_KEY_LEFT_CONTROL};
        for(int i=0;i<keys.length;i++)if(key(window,keys[i]))held|=1<<i;
        if(mc.screen==null&&!alt&&button(window,GLFW.GLFW_MOUSE_BUTTON_LEFT))held|=Controls.FIRE;
        if(mc.screen==null&&!alt&&button(window,GLFW.GLFW_MOUSE_BUTTON_RIGHT))held|=Controls.AIM;
        if(key(window,GLFW.GLFW_KEY_R))held|=Controls.RELOAD;
        for(int i=0;i<4;i++)if(!alt&&key(window,GLFW.GLFW_KEY_1+i))held|=Controls.DAGGER<<i;
        if(key(window,GLFW.GLFW_KEY_Q))held|=Controls.MELEE;
        if(key(window,GLFW.GLFW_KEY_F))held|=Controls.PARRY;
        if(Boolean.getBoolean("haze.lab"))held=HazeLab.held;
        controls=movementKeys.sample(held,mc.player.getYRot(),mc.player.getXRot(),(int)Math.floor(motion.time/MovementClock.INTERVAL));previous=held;
        send(0);
        mc.options.keyAttack.setDown(false);mc.options.keyUse.setDown(false);mc.options.keySprint.setDown(false);mc.options.keyShift.setDown(false);mc.options.keyJump.setDown(false);
        mc.options.keyDrop.setDown(false);while(mc.options.keyDrop.consumeClick()){}
        HazeLab.tick(mc);
    }
    public static void send(int command) {
        send(command,controls.pressed());
    }
    public static void debug(DebugAction action,int value){
        if(DebugScreen.allowed()&&active&&ClientPlayNetworking.canSend(HazeProtocol.Debug.TYPE))
            ClientPlayNetworking.send(new HazeProtocol.Debug(action.ordinal(),value));
    }
    private static void send(int command,int pressed) {
        int held=controls.held();
        if(motion!=null&&motion.mode==Motion.Mode.SLIDE&&controls.down(Controls.CROUCH))held|=Controls.SLIDING;
        if(ClientPlayNetworking.canSend(HazeProtocol.Input.TYPE)) ClientPlayNetworking.send(new HazeProtocol.Input(sequence++,held,pressed,controls.yaw(),controls.pitch(),command));
    }
    @Override public boolean active(Player p) { return HazeClient.active(p.getUUID()); }
    @Override public boolean travel(Player player) {
        if(!(player instanceof LocalPlayer p)||!active||motion==null||p.isPassenger())return false;
        ClientState state=own();
        if(state!=null){if(state.itemMovementSerial!=lastItemMovement){lastItemMovement=state.itemMovementSerial;motion.velocity=new V3(motion.velocity.x(),state.itemImpulse,motion.velocity.z());motion.grounded=false;}
        if(state.itemStaminaSerial!=lastItemStamina){lastItemStamina=state.itemStaminaSerial;motion.stamina=motion.maxStamina();}}
        motion.itemMoveSpeed=0;motion.itemSprintSpeed=0;motion.itemStamina=0;motion.itemStaminaRegenReduction=0;
        if(state!=null){
            var inventory=state.itemInventory;
            motion.itemMoveSpeed=state.itemMoveSpeed;motion.itemSprintSpeed=inventory.stat("BonusSprintSpeed");motion.itemStamina=inventory.stat("Stamina");motion.itemStaminaRegenReduction=inventory.reduction("StaminaCooldownReduction");
        }
        double bonus=state!=null&&state.smoke?Retail.DATA.prop("ability_smoke_bomb","InvisMoveSpeedMod",state.ranks[1]):0;
        double cap=state!=null&&state.flurry?Retail.DATA.prop("ability_bullet_flurry","ChannelMoveSpeed",state.ranks[3]):-1;
        if(playerCollision==null||playerCollision.p!=p||playerCollision.owner!=motion)playerCollision=new PlayerCollision(p,motion);
        Motion.Collision collision=playerCollision;
        Motion.Mode before=motion.mode;
        long jumpBefore=motion.jumpSerial;
        movementClock.advance(.05,controls,(dt,input)->{
            DebugTools.beforeStep();motion.step(dt,input,collision,bonus,cap,state!=null&&state.inCombat);DebugTools.afterStep();
            if(state!=null&&state.itemFlight>0){V3 direction=V3.look(controls.yaw(),controls.pitch());motion.velocity=direction.mul(controls.down(Controls.FORWARD)?state.itemFlightSpeed:0);motion.grounded=false;}
        });
        if(motion.jumpSerial!=jumpBefore&&(before==Motion.Mode.DASH_GROUND||before==Motion.Mode.DASH_AIR))send(210,0);
        if((before==Motion.Mode.SLIDE)!=(motion.mode==Motion.Mode.SLIDE))send(HazeProtocol.SLIDE_STATE_COMMAND,0);
        HazeSounds.movement(before,jumpBefore,motion,controls,p);
        p.setPose(motion.ducked?Pose.CROUCHING:Pose.STANDING);
        p.setOnGround(motion.grounded);
        p.setDeltaMovement(HazeActor.mc(motion.velocity.mul(0.05)));p.fallDistance=0;p.setSprinting(false);
        return true;
    }
    private static final class PlayerCollision implements Motion.Collision {
        private final LocalPlayer p;
        private final Motion owner;
        private final CapsuleMover mover=HazeCollisionWorld.create();
        private final HazeCollisionWorld world;
        PlayerCollision(LocalPlayer p,Motion owner){this.p=p;this.owner=owner;world=new HazeCollisionWorld(p);}
        @Override public boolean canStand(){
            var shape=mover.shape(false);V3 origin=HazeActor.of(p.position());
            for(var box:world.boxes(shape.bounds(origin)))if(CapsuleMover.contact(shape,origin,box).separation()<-1e-6)return false;
            return true;
        }
        @Override public V3 wallContact() {
            return mover.wallContact(HazeActor.of(p.position()),p.getPose()==Pose.CROUCHING,world);
        }
        @Override public boolean meleeContact(V3 direction,double distance){
            Vec3 start=p.getEyePosition().add(0,-p.getEyeHeight()*.4,0),end=start.add(HazeActor.mc(direction.mul(distance)));
            return !p.level().getEntitiesOfClass(Mob.class,new AABB(start,end).inflate(.35),e->e.isAlive()&&p.hasLineOfSight(e)&&e.getBoundingBox().inflate(.35).clip(start,end).isPresent()).isEmpty();
        }
        @Override public Motion.MoveResult move(V3 delta,boolean crouch) {
            return move(delta,crouch,owner.velocity,.015625);
        }
        @Override public Motion.MoveResult move(V3 delta,boolean crouch,V3 velocity,double dt) {
            p.setPose(crouch?Pose.CROUCHING:Pose.STANDING);
            V3 before=HazeActor.of(p.position());
            // Ground dash and mantle supply displacement instead of ordinary velocity.
            boolean scripted=owner.mode==Motion.Mode.DASH_GROUND||owner.mode==Motion.Mode.MANTLE;
            var result=mover.move(before,scripted?delta.mul(1/dt):velocity,owner.grounded,crouch,dt,world,owner.mode==Motion.Mode.MANTLE);
            V3 actual=result.position().sub(before);p.setPos(HazeActor.mc(result.position()));
            boolean wall=result.wallNormal().length()>0;
            p.setOnGroundWithMovement(result.grounded(),wall,HazeActor.mc(actual));p.verticalCollisionBelow=result.grounded();
            return new Motion.MoveResult(actual,result.grounded(),wall,result.wallNormal(),result.slope(),result.velocity());
        }
        @Override public V3 mantle(V3 direction,double maxHeight) {
            if(direction.length()==0)return null;
            double step=Retail.DATA.model.get("m_flStepHeight").getAsDouble()*Retail.SOURCE_UNIT;
            for(double height=step;height<=maxHeight;height+=step) {
                Vec3 target=p.position().add(direction.x(),height,direction.z());
                AABB box=p.getBoundingBox().move(target.subtract(p.position()));
                if(!p.level().noCollision(p,box))continue;
                BlockHitResult hit=p.level().clip(new ClipContext(target,target.add(0,-step,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
                if(hit.getType()!=HitResult.Type.MISS)return HazeActor.of(hit.getLocation().subtract(p.position()));
            }return null;
        }
    }
}
