package org.ppvon.ultimateCobblemonProgression.common.regions;

import java.util.Comparator;
import java.util.List;

/**
 * Loaded regions, in unlock order. Region mode is active whenever at least one region is loaded.
 */
public final class RegionRegistry {
    private static List<RegionDef> REGIONS = List.of();

    private RegionRegistry() {}

    static void set(List<RegionDef> regions) {
        REGIONS = regions.stream()
                .sorted(Comparator.comparingInt((RegionDef r) -> r.order).thenComparing(r -> r.id))
                .toList();
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

    /** Regions players may pick as their extra starting region. The first region is always open, so it's never offered. */
    public static List<RegionDef> startChoices() {
        return REGIONS.stream().skip(1).filter(r -> r.startChoice).toList();
    }
}
