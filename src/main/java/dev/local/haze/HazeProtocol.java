package dev.local.haze;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class HazeProtocol {
    public static final int VERSION=1;
    public static final int SLIDE_STATE_COMMAND=20;
    public static final int DEBUG_REFILL=100,DEBUG_TARGET=101,DEBUG_CLEAR_TARGETS=102,DEBUG_NO_COOLDOWNS=103;
    public record Imbue(String item,int ability) implements CustomPacketPayload {
        public static final Type<Imbue> TYPE=new Type<>(Identifier.fromNamespaceAndPath("haze","imbue_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Imbue> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.item,128);b.writeVarInt(p.ability);},b->new Imbue(b.readUtf(128),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Shop(String item,boolean sell) implements CustomPacketPayload {
        public static final Type<Shop> TYPE=new Type<>(Identifier.fromNamespaceAndPath("haze","shop_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Shop> CODEC=StreamCodec.of((b,p)->{b.writeUtf(p.item,128);b.writeBoolean(p.sell);},b->new Shop(b.readUtf(128),b.readBoolean()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Debug(int action,int value) implements CustomPacketPayload {
        public static final Type<Debug> TYPE=new Type<>(Identifier.fromNamespaceAndPath("haze","debug_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Debug> CODEC=StreamCodec.of((b,p)->{b.writeVarInt(p.action);b.writeVarInt(p.value);},b->new Debug(b.readVarInt(),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
    }
    public record Input(int sequence,int held,int pressed,float yaw,float pitch,int command) implements CustomPacketPayload {
        public static final Type<Input> TYPE=new Type<>(Identifier.fromNamespaceAndPath("haze","input_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Input> CODEC=StreamCodec.of((b,p)->{
            b.writeVarInt(p.sequence);b.writeInt(p.held);b.writeInt(p.pressed);b.writeFloat(p.yaw);b.writeFloat(p.pitch);b.writeVarInt(p.command);
        },b->new Input(b.readVarInt(),b.readInt(),b.readInt(),b.readFloat(),b.readFloat(),b.readVarInt()));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
    public record Snapshot(String json) implements CustomPacketPayload {
        public static final Type<Snapshot> TYPE=new Type<>(Identifier.fromNamespaceAndPath("haze","state_v1"));
        public static final StreamCodec<RegistryFriendlyByteBuf,Snapshot> CODEC=StreamCodec.of((b,p)->b.writeUtf(p.json,32767),b->new Snapshot(b.readUtf(32767)));
        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
