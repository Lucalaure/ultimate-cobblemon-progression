package org.ppvon.ultimateCobblemonProgression.common.gym;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.List;

import static org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression.MOD_ID;

/**
 * Two leagues at once: when both of a player's leagues have a leader ready, using a Gym Challenge block opens a menu
 * to pick which one to battle. The server checks the pick again before the leader is summoned.
 */
public final class GymChoice {
    /** How far from the gym a player may be when they pick from the menu. */
    private static final double MAX_PICK_DISTANCE = 8;

    private GymChoice() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(Open.TYPE, Open.CODEC);
        PayloadTypeRegistry.playC2S().register(Pick.TYPE, Pick.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(Pick.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.distanceToSqr(payload.gym().getCenter()) > MAX_PICK_DISTANCE * MAX_PICK_DISTANCE) return;
            if (player.serverLevel().getBlockEntity(payload.gym()) instanceof GymChallengeBlockEntity gym) {
                gym.challenge(player, payload.trainerId());
            }
        });
    }

    /** The menu's rows, one per ready leader. */
    static List<Option> options(ServerPlayer player, List<GymLeaderPicker.Pick> ready) {
        List<Option> out = new ArrayList<>();
        for (GymLeaderPicker.Pick pick : ready) {
            out.add(new Option(pick.region().title, RctBridge.trainerName(pick.trainerId()), pick.trainerId(),
                    Math.max(0, RctBridge.aceLevel(player.server, pick.trainerId())), GymDifficulty.isHard(pick.trainerId())));
        }
        return out;
    }

    /** A leader the player can challenge: their region, name, strongest Pokémon's level, and whether it's Hard. */
    public record Option(String region, Component name, String trainerId, int aceLevel, boolean hard) {}

    /** Server to client: open the leader menu for the gym at {@code gym}. */
    public record Open(BlockPos gym, List<Option> options) implements CustomPacketPayload {
        public static final Type<Open> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "gym_choice_open"));

        public static final StreamCodec<RegistryFriendlyByteBuf, Open> CODEC = StreamCodec.of(
                (buf, p) -> {
                    buf.writeBlockPos(p.gym);
                    buf.writeVarInt(p.options.size());
                    for (Option o : p.options) {
                        buf.writeUtf(o.region);
                        ComponentSerialization.STREAM_CODEC.encode(buf, o.name);
                        buf.writeUtf(o.trainerId);
                        buf.writeVarInt(o.aceLevel);
                        buf.writeBoolean(o.hard);
                    }
                },
                buf -> {
                    BlockPos gym = buf.readBlockPos();
                    List<Option> options = new ArrayList<>();
                    for (int i = buf.readVarInt(); i > 0; i--) {
                        options.add(new Option(buf.readUtf(), ComponentSerialization.STREAM_CODEC.decode(buf), buf.readUtf(),
                                buf.readVarInt(), buf.readBoolean()));
                    }
                    return new Open(gym, options);
                });

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    /** Client to server: the player picked a leader to challenge at the gym at {@code gym}. */
    public record Pick(BlockPos gym, String trainerId) implements CustomPacketPayload {
        public static final Type<Pick> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "gym_choice_pick"));

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
