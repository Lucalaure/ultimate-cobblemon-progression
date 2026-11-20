package org.ppvon.ultimateCobblemonProgression.common.gym;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

import static org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression.MOD_ID;

/** The network messages behind a Gym Challenge block's rematch menu. */
public final class RematchPayloads {
    private RematchPayloads() {}

    /**
     * A leader the player can rematch.
     *
     * @param hardId the slot's Hard version, or "" when it has none
     */
    public record Entry(String region, Component name, String normalId, String hardId) {}

    /** Server to client: open the rematch menu for the gym at {@code gym}. */
    public record Open(BlockPos gym, List<Entry> entries) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "rematch_open"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBlockPos(p.gym);
                    buf.writeVarInt(p.entries.size());
                    for (Entry e : p.entries) {
                        buf.writeUtf(e.region);
                        ComponentSerialization.STREAM_CODEC.encode(buf, e.name);
                        buf.writeUtf(e.normalId);
                        buf.writeUtf(e.hardId);
                    }
                },
                buf -> {
                    BlockPos gym = buf.readBlockPos();
                    List<Entry> entries = new ArrayList<>();
                    for (int i = buf.readVarInt(); i > 0; i--) {
                        entries.add(new Entry(buf.readUtf(), ComponentSerialization.STREAM_CODEC.decode(buf), buf.readUtf(), buf.readUtf()));
                    }
                    return new Open(gym, entries);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: the player picked a leader to rematch at the gym at {@code gym}. */
    public record Pick(BlockPos gym, String trainerId) implements CustomPacketPayload {
        public static final Type<Pick> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "rematch_pick"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Pick> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBlockPos(p.gym);
                    buf.writeUtf(p.trainerId, 128);
                },
                buf -> new Pick(buf.readBlockPos(), buf.readUtf(128)));

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }
}
