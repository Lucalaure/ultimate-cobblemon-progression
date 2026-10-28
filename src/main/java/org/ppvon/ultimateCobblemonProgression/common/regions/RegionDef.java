package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;

/**
 * A region (e.g. Kanto) with its own ladder of tiers. Loaded from
 * data/ultimate-cobblemon-progression/regions/&lt;id&gt;/region.json plus tier_N.json files.
 */
public final class RegionDef {
    public final String id;
    public final int order;
    public final String title;
    /** Advancement that marks this region as complete and unlocks the next one. Empty = completing the last tier. */
    public final Optional<ResourceLocation> completedBy;
    /** Whether players may pick this region as their extra region that's open from the start. */
    public final boolean startChoice;
    /** Tier index (1-based, consecutive) to tier definition. */
    public final SortedMap<Integer, TierDef> tiers;
    private final Map<Integer, Integer> realSpeciesByTier = new HashMap<>();

    public RegionDef(String id, Meta meta, SortedMap<Integer, TierDef> tiers) {
        this.id = id;
        this.order = meta.order;
        this.title = meta.title.orElse(id);
        this.completedBy = meta.completedBy;
        this.startChoice = meta.startChoice;
        this.tiers = Collections.unmodifiableSortedMap(tiers);
    }

    public int maxTier() {
        return tiers.isEmpty() ? 0 : tiers.lastKey();
    }

    public int levelCap(int tier) {
        TierDef def = tiers.get(tier);
        return def != null ? def.levelCap : 100;
    }

    /** Number of species from this tier that exist in the loaded Cobblemon species registry. */
    public int realSpecies(int tier) {
        return realSpeciesByTier.getOrDefault(tier, 0);
    }

    void putRealSpecies(int tier, int count) {
        realSpeciesByTier.put(tier, count);
    }

    /** Contents of region.json. */
    public record Meta(int order, Optional<String> title, Optional<ResourceLocation> completedBy, boolean startChoice) {
        public static final Codec<Meta> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("order", 0).forGetter(Meta::order),
                Codec.STRING.optionalFieldOf("title").forGetter(Meta::title),
                ResourceLocation.CODEC.optionalFieldOf("completedBy").forGetter(Meta::completedBy),
                Codec.BOOL.optionalFieldOf("startChoice", false).forGetter(Meta::startChoice)
        ).apply(i, Meta::new));
    }
}
