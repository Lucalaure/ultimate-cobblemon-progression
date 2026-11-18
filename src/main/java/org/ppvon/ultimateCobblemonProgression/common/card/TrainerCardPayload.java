package org.ppvon.ultimateCobblemonProgression.common.card;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression.MOD_ID;

/** Server to client: a player's Trainer Card, shown in the Trainer Card screen. */
public record TrainerCardPayload(TrainerCardData data) implements CustomPacketPayload {
    public static final Type<TrainerCardPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "trainer_card"));

    public static final StreamCodec<RegistryFriendlyByteBuf, TrainerCardPayload> CODEC =
            TrainerCardData.CODEC.map(TrainerCardPayload::new, TrainerCardPayload::data);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
