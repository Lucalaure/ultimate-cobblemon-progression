package org.ppvon.ultimateCobblemonProgression.common.card;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression.MOD_ID;

/**
 * Client to server: the player picked a region on the Trainer Card.
 *
 * @param home true for the region their journey starts in, false for the optional second region
 */
public record PickRegionPayload(String regionId, boolean home) implements CustomPacketPayload {
    public static final Type<PickRegionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "pick_region"));

    public static final StreamCodec<FriendlyByteBuf, PickRegionPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.regionId, 64);
                buf.writeBoolean(p.home);
            },
            buf -> new PickRegionPayload(buf.readUtf(64), buf.readBoolean()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
