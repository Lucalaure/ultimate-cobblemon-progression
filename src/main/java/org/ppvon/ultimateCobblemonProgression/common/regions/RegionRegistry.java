package org.ppvon.ultimateCobblemonProgression.common.regions;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Loaded regions, in unlock order. Region mode is active whenever at least one region is loaded.
 */
public final class RegionRegistry {
    private static List<RegionDef> REGIONS = List.of();
    /** Every RCT trainer id listed in a region's tiers: the trainers of UCP's leagues. */
    private static Set<String> LEAGUE_TRAINERS = Set.of();

    private RegionRegistry() {}

    static void set(List<RegionDef> regions) {
        REGIONS = regions.stream()
                .sorted(Comparator.comparingInt((RegionDef r) -> r.order).thenComparing(r -> r.id))
                .toList();
        Set<String> trainers = new HashSet<>();
        for (RegionDef region : REGIONS) {
            region.tiers.values().forEach(tier -> tier.trainers.forEach(trainers::addAll));
        }
        LEAGUE_TRAINERS = Set.copyOf(trainers);
    }

    /** Whether the RCT trainer is one of UCP's league trainers (listed in some region's tiers). */
    public static boolean isLeagueTrainer(String trainerId) {
        return LEAGUE_TRAINERS.contains(trainerId);
    }

    public static boolean isActive() {
        return !REGIONS.isEmpty();
    }

    /** Regions in unlock order. A species' region index refers to this list. */
    public static List<RegionDef> ordered() {
        return REGIONS;
    }

    public static RegionDef get(int index) {
        return index >= 0 && index < REGIONS.size() ? REGIONS.get(index) : null;
    }

    public static RegionDef byId(String id) {
        return REGIONS.stream().filter(r -> r.id.equals(id)).findFirst().orElse(null);
    }

    /**
     * League regions: the ones with a trainer series. A player's journey starts in one of them (their home region)
     * and moves on to another each time they complete every league region on it.
     */
    public static List<RegionDef> homeChoices() {
        return REGIONS.stream().filter(RegionDef::isLeague).toList();
    }

    /**
     * Regions players may pick as their extra starting region. The first region is always open, so it's never offered,
     * and league regions open through the journey instead.
     */
    public static List<RegionDef> startChoices() {
        return REGIONS.stream().skip(1).filter(r -> r.startChoice && !r.isLeague()).toList();
    }
}
