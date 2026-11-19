package org.ppvon.ultimateCobblemonProgression.common.card;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import static org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression.MOD_ID;

/**
 * Client to server: the player picked a region on the Trainer Card.
 *
 * @param kind {@link #HOME} for the region their journey starts in, {@link #NEXT} for the region it continues in,
 *             {@link #EXTRA} for the optional second region
 */
public record PickRegionPayload(String regionId, int kind) implements CustomPacketPayload {
    public static final int EXTRA = 0, HOME = 1, NEXT = 2;

    public static final Type<PickRegionPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(MOD_ID, "pick_region"));

    public static final StreamCodec<FriendlyByteBuf, PickRegionPayload> CODEC = StreamCodec.of(
            (buf, p) -> {
                buf.writeUtf(p.regionId, 64);
                buf.writeVarInt(p.kind);
            },
            buf -> new PickRegionPayload(buf.readUtf(64), buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
