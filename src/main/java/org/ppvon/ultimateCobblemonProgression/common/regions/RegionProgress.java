package org.ppvon.ultimateCobblemonProgression.common.regions;

import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.progression.RequirementCheck;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;

import java.util.ArrayList;
import java.util.List;

/**
 * A player's progress through every region, derived from their advancements, Pokédex and region picks.
 * Only the picks and the highest tier reached per region are saved, so resetting a series in another mod can never
 * re-lock a region. The reached tier only waives the catchWholeTier check: requirements like badges still apply.
 *
 * <p>Which regions are open:
 * <ul>
 *   <li>Before a player picks their home region (or when no region has a trainer series), the regions open in
 *       order: the first one from the start, each later one once the one before it is complete.</li>
 *   <li>Once they've picked, only their <b>journey</b> opens regions: their home region, and each region they
 *       picked as the next stop after completing the one before. Nothing else opens on its own.</li>
 *   <li>The extra starting region, if picked, is always open.</li>
 * </ul>
 */
public final class RegionProgress {
    /** Highest unlocked tier per region; 0 when the region (or its first tier) is locked. */
    private final int[] steps;
    private final boolean[] unlocked;
    private final boolean[] completed;
    /** With catchWholeTier: whether every obtainable Pokémon of the region's current tier is caught. */
    private final boolean[] tierCaught;
    /** Region indexes of the player's journey: home first, then each next region in the order picked. */
    private final List<Integer> journey = new ArrayList<>();
    /** The player's second region (their extra starting pick), or -1. */
    private int second = -1;

    private RegionProgress(int size) {
        steps = new int[size];
        unlocked = new boolean[size];
        completed = new boolean[size];
        tierCaught = new boolean[size];
    }

    public static RegionProgress compute(ServerPlayer player) {
        List<RegionDef> regions = RegionRegistry.ordered();
        RegionProgress p = new RegionProgress(regions.size());
        var dex = new RequirementCheck.DexLookup(player);
        String startRegion = chosenStartRegion(player);

        for (String id : journeyIds(player)) p.journey.add(regions.indexOf(RegionRegistry.byId(id)));

        if (p.journey.isEmpty()) {
            // No journey yet: the first region is open, and each region opens once the one before it is complete.
            boolean previousCompleted = true;
            for (int r = 0; r < regions.size(); r++) {
                if (regions.get(r).id.equals(startRegion)) p.second = r;
                boolean open = previousCompleted || p.second == r;
                previousCompleted = open && p.fill(player, r, dex);
            }
            return p;
        }

        // With a journey, only its regions and the extra starting region are open.
        for (int r : p.journey) p.fill(player, r, dex);
        int start = regions.indexOf(RegionRegistry.byId(startRegion));
        if (start >= 0) {
            p.second = start;
            p.fill(player, start, dex);
        }
        return p;
    }

    /** Opens a region and works out its tier and completion. Returns whether it's complete. */
    private boolean fill(ServerPlayer player, int r, RequirementCheck.DexLookup dex) {
        RegionDef region = RegionRegistry.get(r);
        unlocked[r] = true;
        var component = TrainerLevelComponents.KEY.get(player);
        int reached = component.getReachedTier(region.id);
        if (reached < 0) {
            // Not saved yet (a world from before CJP saved it): the tier the player had then, when only species
            // counted for catchWholeTier, so the regional forms added since don't take a tier away.
            reached = TierCatches.isReady() ? tierFor(player, region, dex, 0, true) : 0;
        }
        int step = tierFor(player, region, dex, reached, false);
        if (step > reached || component.getReachedTier(region.id) < 0) {
            if (TierCatches.isReady()) component.setReachedTier(region.id, Math.max(step, reached));
        }
        steps[r] = step;
        tierCaught[r] = step > 0 && TierCatches.applies(region) && TierCatches.missing(player, region, step).isEmpty();
        completed[r] = region.completedBy
                .map(id -> RequirementCheck.isAdvancementDone(player, id))
                .orElse(step == region.maxTier());
        return completed[r];
    }

    /**
     * The highest tier whose requirements the player meets, with catchWholeTier holding back every tier above
     * {@code reached} until the tier before it is caught ({@code legacy}: counting species only, as CJP did before
     * regional forms were caught on their own).
     */
    private static int tierFor(ServerPlayer player, RegionDef region, RequirementCheck.DexLookup dex, int reached,
                               boolean legacy) {
        int step = 0;
        for (TierDef tier : region.tiers.values()) {
            if (!RequirementCheck.isMet(player, tier.requirements, dex)) break;
            // catchWholeTier: the tier before this one must be fully caught too, unless the player already reached
            // this tier (Pokémon added to a tier later never take a tier away).
            if (tier.index > 1 && tier.index > reached) {
                var missing = legacy ? TierCatches.missingSpecies(player, region, tier.index - 1)
                        : TierCatches.missing(player, region, tier.index - 1);
                if (!missing.isEmpty()) break;
            }
            step = tier.index;
        }
        return step;
    }

    /** The player's home region (where their journey starts), if it's still one of the home choices. */
    public static String chosenHomeRegion(ServerPlayer player) {
        String id = TrainerLevelComponents.KEY.get(player).getHomeRegion().orElse(null);
        RegionDef def = id != null ? RegionRegistry.byId(id) : null;
        return def != null && def.isLeague() ? id : null;
    }

    /**
     * The player's journey as region ids: their home region, then every next region they picked, in order.
     * Picks that are no longer league regions (e.g. after a datapack change) are left out. Empty until a home is picked.
     */
    public static List<String> journeyIds(ServerPlayer player) {
        String home = chosenHomeRegion(player);
        if (home == null) return List.of();
        List<String> ids = new ArrayList<>();
        ids.add(home);
        for (String id : TrainerLevelComponents.KEY.get(player).getNextRegions()) {
            RegionDef def = RegionRegistry.byId(id);
            if (def != null && def.isLeague() && !ids.contains(id)) ids.add(id);
        }
        return ids;
    }

    /** Whether the player still has to pick their home region: region mode is on, there's a choice, and they haven't. */
    public static boolean needsHomeRegion(ServerPlayer player) {
        return RegionRegistry.isActive() && !RegionRegistry.homeChoices().isEmpty() && chosenHomeRegion(player) == null;
    }

    /**
     * The league regions the player can travel to next: every one not yet on their journey (or their second region),
     * once every league they're working through is complete. Two leagues progress as a pair: when one finishes first,
     * the player carries on with the other, and picks what comes next once both are done, when their level cap starts
     * over with the new region. Empty before they've picked a home.
     */
    public static List<RegionDef> nextChoices(ServerPlayer player) {
        RegionProgress progress = RegionProgressTracker.get(player);
        if (progress.journey.isEmpty() || !progress.activeLeagues().isEmpty()) return List.of();
        List<String> journey = journeyIds(player);
        String second = chosenStartRegion(player);
        return RegionRegistry.homeChoices().stream()
                .filter(r -> !journey.contains(r.id) && !r.id.equals(second)).toList();
    }

    /** Whether the player has completed their journey so far and has a next region to pick. */
    public static boolean needsNextRegion(ServerPlayer player) {
        return RegionRegistry.isActive() && !nextChoices(player).isEmpty();
    }

    /**
     * The extra starting regions the player can still pick: none once they've picked, otherwise every start choice
     * that isn't open already (after completing their home region, most of them open on their own).
     */
    public static List<RegionDef> openableStartChoices(ServerPlayer player) {
        if (chosenStartRegion(player) != null) return List.of();
        RegionProgress progress = RegionProgressTracker.get(player);
        List<RegionDef> regions = RegionRegistry.ordered();
        // A league as the second region only once the journey has a home league to go alongside.
        boolean leagues = chosenHomeRegion(player) != null;
        return RegionRegistry.startChoices().stream()
                .filter(r -> (leagues || !r.isLeague()) && !progress.isUnlocked(regions.indexOf(r))).toList();
    }

    /** The player's second region (their extra starting pick), if it still counts as one. */
    public static String chosenStartRegion(ServerPlayer player) {
        String id = TrainerLevelComponents.KEY.get(player).getStartRegion().orElse(null);
        RegionDef def = id != null ? RegionRegistry.byId(id) : null;
        return def != null && RegionRegistry.isSecondRegionChoice(def) && !id.equals(chosenHomeRegion(player)) ? id : null;
    }

    public int size() {
        return steps.length;
    }

    /** Highest unlocked tier in the region, 0 if none. */
    public int step(int region) {
        return region >= 0 && region < steps.length ? steps[region] : 0;
    }

    public boolean isUnlocked(int region) {
        return region >= 0 && region < unlocked.length && unlocked[region];
    }

    public boolean isCompleted(int region) {
        return region >= 0 && region < completed.length && completed[region];
    }

    /** With catchWholeTier on: whether the player has caught every obtainable Pokémon of the region's current tier. */
    public boolean isTierCaught(int region) {
        return region >= 0 && region < tierCaught.length && tierCaught[region];
    }

    /** Region indexes of the journey, home first. Empty until the player picks a home region. */
    public List<Integer> journey() {
        return List.copyOf(journey);
    }

    public boolean isOnJourney(int region) {
        return journey.contains(region);
    }

    /** The region the player is working through: the first of {@link #activeLeagues()}, or -1. */
    public int current() {
        List<Integer> active = activeLeagues();
        return active.isEmpty() ? -1 : active.get(0);
    }

    /** The player's second region (their extra starting pick), or -1. */
    public int secondRegion() {
        return second;
    }

    /** Whether the player's second region is a league, so they work through two leagues side by side. */
    public boolean hasSecondLeague() {
        RegionDef def = RegionRegistry.get(second);
        return def != null && def.isLeague();
    }

    /** The leagues the player is working through: unfinished journey regions, then an unfinished second league. */
    public List<Integer> activeLeagues() {
        List<Integer> active = new ArrayList<>();
        for (int r : journey) {
            if (!completed[r]) active.add(r);
        }
        if (hasSecondLeague() && !completed[second] && !active.contains(second)) active.add(second);
        return active;
    }

    /** Whether the region is on the player's journey or is their second region. */
    public boolean isPlayerPick(int region) {
        return journey.contains(region) || (region >= 0 && region == second);
    }

    /**
     * Region indexes with the journey first (in journey order), then the second region, then every other region in
     * unlock order.
     * Used to decide which region's next trainer a gym summons, and which region the Trainer Card shows.
     */
    public List<Integer> priorityOrder() {
        List<Integer> order = new ArrayList<>(journey);
        if (second >= 0 && !order.contains(second)) order.add(second);
        for (int r = 0; r < steps.length; r++) {
            if (!order.contains(r)) order.add(r);
        }
        return order;
    }
}
