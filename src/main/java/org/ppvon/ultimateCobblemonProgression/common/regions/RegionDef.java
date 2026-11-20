package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.ResourceLocation;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;

import com.cobblemon.mod.common.pokemon.Species;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedMap;
import java.util.stream.Collectors;

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
    /**
     * The Radical Cobblemon Trainers series for this region's trainers (e.g. "kanto"). A region with a series can be
     * picked as a player's home region: where their journey starts, joining this series.
     */
    public final Optional<String> series;
    /**
     * Cobblemon aspects that mark a Pokémon as one of this region's forms (e.g. "alolan", or "region-bias-alola" for a
     * Pikachu that will evolve into Alolan Raichu). With lockRegionalForms on, such a Pokémon only spawns for players
     * who have this region open, on top of its species' own tier and region.
     */
    public final Set<String> formAspects;
    /** Tier index (1-based, consecutive) to tier definition. */
    public final SortedMap<Integer, TierDef> tiers;
    private final Map<Integer, Integer> realSpeciesByTier = new HashMap<>();
    /** The Cobblemon species belonging to each tier of this region (a species listed in several regions counts in the first). */
    private final Map<Integer, List<Species>> speciesByTier = new HashMap<>();

    public RegionDef(String id, Meta meta, SortedMap<Integer, TierDef> tiers) {
        this.id = id;
        this.order = meta.order;
        this.title = meta.title.orElse(id);
        this.completedBy = meta.completedBy;
        this.startChoice = meta.startChoice;
        this.series = meta.series;
        this.formAspects = meta.formAspects.stream()
                .map(a -> a.toLowerCase(Locale.ROOT))
                .collect(Collectors.toUnmodifiableSet());
        this.tiers = Collections.unmodifiableSortedMap(tiers);
    }

    /** Whether this is a league region: one with a trainer series, which players travel to on their journey. */
    public boolean isLeague() {
        return series.isPresent();
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

    /** The loaded Cobblemon species that belong to this tier of the region. */
    public List<Species> species(int tier) {
        return speciesByTier.getOrDefault(tier, List.of());
    }

    void clearSpecies() {
        speciesByTier.clear();
    }

    void addSpecies(int tier, Species species) {
        speciesByTier.computeIfAbsent(tier, t -> new ArrayList<>()).add(species);
    }

    /** Contents of region.json. */
    public record Meta(int order, Optional<String> title, Optional<ResourceLocation> completedBy, boolean startChoice,
                       Optional<String> series, List<String> formAspects) {
        public static final Codec<Meta> CODEC = RecordCodecBuilder.create(i -> i.group(
                Codec.INT.optionalFieldOf("order", 0).forGetter(Meta::order),
                Codec.STRING.optionalFieldOf("title").forGetter(Meta::title),
                ResourceLocation.CODEC.optionalFieldOf("completedBy").forGetter(Meta::completedBy),
                Codec.BOOL.optionalFieldOf("startChoice", false).forGetter(Meta::startChoice),
                Codec.STRING.optionalFieldOf("series").forGetter(Meta::series),
                Codec.STRING.listOf().optionalFieldOf("formAspects", List.of()).forGetter(Meta::formAspects)
        ).apply(i, Meta::new));
    }
}
