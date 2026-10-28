package org.ppvon.ultimateCobblemonProgression.common.regions;

import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.progression.RequirementCheck;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;

import java.util.List;

/**
 * A player's progress through every region, derived entirely from their advancements and Pokédex.
 * Nothing here is saved, so resetting a series in another mod can never re-lock a region.
 */
public final class RegionProgress {
    /** Highest unlocked tier per region; 0 when the region (or its first tier) is locked. */
    private final int[] steps;
    private final boolean[] unlocked;
    private final boolean[] completed;

    private RegionProgress(int size) {
        steps = new int[size];
        unlocked = new boolean[size];
        completed = new boolean[size];
    }

    public static RegionProgress compute(ServerPlayer player) {
        List<RegionDef> regions = RegionRegistry.ordered();
        RegionProgress p = new RegionProgress(regions.size());
        var dex = new RequirementCheck.DexLookup(player);
        String startRegion = chosenStartRegion(player);

        boolean previousCompleted = true;
        for (int r = 0; r < regions.size(); r++) {
            RegionDef region = regions.get(r);
            p.unlocked[r] = previousCompleted || region.id.equals(startRegion);
            if (!p.unlocked[r]) {
                previousCompleted = false;
                continue;
            }

            int step = 0;
            for (TierDef tier : region.tiers.values()) {
                if (!RequirementCheck.isMet(player, tier.requirements, dex)) break;
                step = tier.index;
            }
            p.steps[r] = step;
            p.completed[r] = region.completedBy
                    .map(id -> RequirementCheck.isAdvancementDone(player, id))
                    .orElse(step == region.maxTier());
            previousCompleted = p.completed[r];
        }
        return p;
    }

    /** The player's chosen extra starting region, if it's still offered as a start choice. */
    public static String chosenStartRegion(ServerPlayer player) {
        String id = TrainerLevelComponents.KEY.get(player).getStartRegion().orElse(null);
        RegionDef def = id != null ? RegionRegistry.byId(id) : null;
        return def != null && RegionRegistry.startChoices().contains(def) ? id : null;
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
}
