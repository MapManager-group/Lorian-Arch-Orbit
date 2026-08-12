package com.davidblackcn.lorianarchorbit.reach;

import com.davidblackcn.lorianarchorbit.LorianArchOrbit;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public final class ReachPackets {
    private ReachPackets() {
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(LorianArchOrbit.MOD_ID, path);
    }

    public record Hello(int version) implements CustomPacketPayload {
        public static final Type<Hello> TYPE = new Type<>(id("reach/hello"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Hello> CODEC = CustomPacketPayload.codec(
                (value, buffer) -> buffer.writeVarInt(value.version),
                buffer -> new Hello(buffer.readVarInt())
        );

        @Override public Type<Hello> type() { return TYPE; }
    }

    public record Capabilities(int version, boolean available, int maximumDistance, int confirmedDistance,
                               ReachDecision decision) implements CustomPacketPayload {
        public static final Type<Capabilities> TYPE = new Type<>(id("reach/capabilities"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Capabilities> CODEC = CustomPacketPayload.codec(
                (value, buffer) -> {
                    buffer.writeVarInt(value.version);
                    buffer.writeBoolean(value.available);
                    buffer.writeVarInt(value.maximumDistance);
                    buffer.writeVarInt(value.confirmedDistance);
                    buffer.writeVarInt(value.decision.ordinal());
                },
                buffer -> new Capabilities(buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(),
                        buffer.readVarInt(), ReachDecision.byId(buffer.readVarInt()))
        );

        @Override public Type<Capabilities> type() { return TYPE; }
    }

    public record SetDistance(int version, int requestedDistance) implements CustomPacketPayload {
        public static final Type<SetDistance> TYPE = new Type<>(id("reach/set_block_reach"));
        public static final StreamCodec<RegistryFriendlyByteBuf, SetDistance> CODEC = CustomPacketPayload.codec(
                (value, buffer) -> {
                    buffer.writeVarInt(value.version);
                    buffer.writeVarInt(value.requestedDistance);
                },
                buffer -> new SetDistance(buffer.readVarInt(), buffer.readVarInt())
        );

        @Override public Type<SetDistance> type() { return TYPE; }
    }

    public record Result(int version, boolean accepted, int confirmedDistance,
                         ReachDecision decision) implements CustomPacketPayload {
        public static final Type<Result> TYPE = new Type<>(id("reach/result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, Result> CODEC = CustomPacketPayload.codec(
                (value, buffer) -> {
                    buffer.writeVarInt(value.version);
                    buffer.writeBoolean(value.accepted);
                    buffer.writeVarInt(value.confirmedDistance);
                    buffer.writeVarInt(value.decision.ordinal());
                },
                buffer -> new Result(buffer.readVarInt(), buffer.readBoolean(), buffer.readVarInt(),
                        ReachDecision.byId(buffer.readVarInt()))
        );

        @Override public Type<Result> type() { return TYPE; }
    }
}
