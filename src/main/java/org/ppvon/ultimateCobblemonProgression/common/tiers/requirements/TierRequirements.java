package org.ppvon.ultimateCobblemonProgression.common.tiers.requirements;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class TierRequirements {
    public final DexRequirements dex;
    /**
     * Advancements that must all be completed, e.g. "rctmod:trainers/defeat_leader_brock". Each entry is a group of
     * alternatives: completing any one advancement in a group counts for that group.
     */
    public final List<List<ResourceLocation>> advancements;
    public static final TierRequirements NONE = new TierRequirements(DexRequirements.NONE, List.of());


    public TierRequirements(DexRequirements dex, List<List<ResourceLocation>> advancements) {
        this.dex = dex != null ? dex : DexRequirements.NONE;
        this.advancements = advancements != null
                ? advancements.stream().filter(g -> !g.isEmpty()).map(List::copyOf).toList()
                : List.of();
    }

    public boolean hasAny() {
        return dex.hasAny() || !advancements.isEmpty();
    }

    /** An advancement id, or a list of alternative advancement ids. */
    private static final Codec<List<ResourceLocation>> ADVANCEMENT_GROUP_CODEC =
            Codec.either(ResourceLocation.CODEC, ResourceLocation.CODEC.listOf()).xmap(
                    e -> e.map(List::of, l -> l),
                    l -> l.size() == 1 ? Either.left(l.get(0)) : Either.right(l)
            );

    public static final Codec<TierRequirements> CODEC = RecordCodecBuilder.create(i -> i.group(
            DexRequirements.CODEC.optionalFieldOf("dex", DexRequirements.NONE).forGetter(r -> r.dex),
            ADVANCEMENT_GROUP_CODEC.listOf().optionalFieldOf("advancements", List.of()).forGetter(r -> r.advancements)
    ).apply(i, TierRequirements::new));
}
