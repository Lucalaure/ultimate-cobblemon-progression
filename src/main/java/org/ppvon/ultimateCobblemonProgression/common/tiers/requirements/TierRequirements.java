package org.ppvon.ultimateCobblemonProgression.common.tiers.requirements;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public final class TierRequirements {
    public final DexRequirements dex;
    /** Advancements that must all be completed, e.g. "rctmod:trainers/defeat_leader_brock". */
    public final List<ResourceLocation> advancements;
    public static final TierRequirements NONE = new TierRequirements(DexRequirements.NONE, List.of());


    public TierRequirements(DexRequirements dex, List<ResourceLocation> advancements) {
        this.dex = dex != null ? dex : DexRequirements.NONE;
        this.advancements = advancements != null ? List.copyOf(advancements) : List.of();
    }

    public boolean hasAny() {
        return dex.hasAny() || !advancements.isEmpty();
    }

    public static final Codec<TierRequirements> CODEC = RecordCodecBuilder.create(i -> i.group(
            DexRequirements.CODEC.optionalFieldOf("dex", DexRequirements.NONE).forGetter(r -> r.dex),
            ResourceLocation.CODEC.listOf().optionalFieldOf("advancements", List.of()).forGetter(r -> r.advancements)
    ).apply(i, TierRequirements::new));
}
