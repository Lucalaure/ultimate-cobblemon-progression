package org.ppvon.ultimateCobblemonProgression.common.regions;

import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Loaded regions, in unlock order. Region mode is active whenever at least one region is loaded.
 */
public final class RegionRegistry {
    private static List<RegionDef> REGIONS = List.of();
    /** Every RCT trainer id listed in a region's tiers: the trainers of UCP's leagues. */
    private static Set<String> LEAGUE_TRAINERS = Set.of();
    /** Each regional-form aspect to the index of the region it belongs to. */
    private static Map<String, Integer> FORM_ASPECT_REGIONS = Map.of();
    /** Each RCT wild trainer listed in a region's wild_trainers.json to that region's index. */
    private static Map<String, Integer> WILD_TRAINER_REGIONS = Map.of();

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

        Map<String, Integer> formAspects = new HashMap<>();
        for (int r = 0; r < REGIONS.size(); r++) {
            RegionDef region = REGIONS.get(r);
            for (String aspect : region.formAspects) {
                Integer previous = formAspects.putIfAbsent(aspect, r);
                if (previous != null && previous != r) {
                    UltimateCobblemonProgression.LOG.warn("Form aspect '{}' is listed in regions '{}' and '{}'; keeping '{}'",
                            aspect, REGIONS.get(previous).id, region.id, REGIONS.get(previous).id);
                }
            }
        }
        FORM_ASPECT_REGIONS = Map.copyOf(formAspects);

        Map<String, Integer> wildTrainers = new HashMap<>();
        for (int r = 0; r < REGIONS.size(); r++) {
            RegionDef region = REGIONS.get(r);
            for (List<String> versions : region.wildTrainers) {
                for (String trainerId : versions) {
                    Integer previous = wildTrainers.putIfAbsent(trainerId, r);
                    if (previous != null && previous != r) {
                        UltimateCobblemonProgression.LOG.warn("Wild trainer '{}' is listed in regions '{}' and '{}'; keeping '{}'",
                                trainerId, REGIONS.get(previous).id, region.id, REGIONS.get(previous).id);
                    }
                }
            }
        }
        WILD_TRAINER_REGIONS = Map.copyOf(wildTrainers);
    }

    /** The index of the region whose wild_trainers.json lists this RCT trainer, or -1 if none does. */
    public static int wildTrainerRegion(String trainerId) {
        Integer r = WILD_TRAINER_REGIONS.get(trainerId);
        return r != null ? r : -1;
    }

    /**
     * The region whose regional form these aspects mark (see {@link RegionDef#formAspects}), or -1 if none does.
     * If several match, the earliest region wins.
     */
    public static int formRegion(Collection<String> aspects) {
        if (FORM_ASPECT_REGIONS.isEmpty()) return -1;
        int region = -1;
        for (String aspect : aspects) {
            Integer r = FORM_ASPECT_REGIONS.get(aspect);
            if (r != null && (region < 0 || r < region)) region = r;
        }
        return region;
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
     * Regions players may pick as their second region: regions without gyms marked "startChoice", and, with
     * allowTwoLeagues on, every league, so two leagues can be worked through side by side.
     */
    public static List<RegionDef> startChoices() {
        boolean leagues = ConfigLoader.ALLOW_TWO_LEAGUES.get();
        return REGIONS.stream().filter(r -> r.isLeague() ? leagues : r.startChoice).toList();
    }

    /**
     * Whether a player's saved second-region pick still counts: any league (even with allowTwoLeagues turned off
     * since, so no one loses a league they're working through), or a region without gyms marked "startChoice".
     */
    public static boolean isSecondRegionChoice(RegionDef region) {
        return region != null && (region.isLeague() || region.startChoice);
    }
}
