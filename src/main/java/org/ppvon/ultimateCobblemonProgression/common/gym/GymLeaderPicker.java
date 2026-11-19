package org.ppvon.ultimateCobblemonProgression.common.gym;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionDef;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgress;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgressTracker;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;

import java.util.List;

/**
 * Works out which trainer a Gym Challenge block should summon for a player: the first trainer they
 * haven't beaten yet in the next locked tier of an open, unfinished region that lists trainers, trying the
 * journey's current region first.
 */
public final class GymLeaderPicker {
    private GymLeaderPicker() {}

    /** Either a trainer to summon, or a message saying why there's none. */
    public record Pick(String trainerId, RegionDef region, Component problem) {
        static Pick of(String trainerId, RegionDef region) { return new Pick(trainerId, region, null); }
        static Pick none(Component why) { return new Pick(null, null, why); }
        public boolean found() { return trainerId != null; }
    }

    public static Pick pick(ServerPlayer player) {
        if (!RegionRegistry.isActive()) {
            return Pick.none(Component.literal("Gyms only work when region data is loaded."));
        }
        if (!RctBridge.isLoaded()) {
            return Pick.none(Component.literal("Gyms need Radical Cobblemon Trainers to be installed."));
        }

        if (RegionProgress.needsHomeRegion(player)) {
            return Pick.none(Component.literal("Choose where your journey starts first: open your Trainer Card."));
        }

        RegionProgress progress = RegionProgressTracker.get(player);
        List<RegionDef> regions = RegionRegistry.ordered();
        boolean allBeatenSomewhere = false;

        // The journey's current region comes first, then any other open region with trainers.
        for (int r : progress.priorityOrder()) {
            if (!progress.isUnlocked(r) || progress.isCompleted(r)) continue;
            RegionDef region = regions.get(r);
            TierDef next = region.tiers.get(progress.step(r) + 1);
            if (next == null || next.trainers.isEmpty()) continue;

            for (List<String> group : next.trainers) {
                boolean beaten = false;
                for (String trainerId : group) {
                    int defeats = RctBridge.defeats(player, trainerId);
                    if (defeats < 0) {
                        return Pick.none(Component.literal("Couldn't look up the trainer '" + trainerId
                                + "' in Radical Cobblemon Trainers. Check the region data and the server log."));
                    }
                    if (defeats > 0) {
                        beaten = true;
                        break;
                    }
                }
                if (!beaten) return Pick.of(group.get(0), region);
            }
            // Every listed trainer is beaten but the tier is still locked, so its other requirements aren't met.
            allBeatenSomewhere = true;
        }

        if (RegionProgress.needsNextRegion(player)) {
            return Pick.none(Component.literal("You've completed your region! Choose where your journey goes next: open your Trainer Card."));
        }
        if (allBeatenSomewhere) {
            return Pick.none(Component.literal("You've beaten every trainer for your next step. Meet its other requirements to move on."));
        }
        return Pick.none(Component.literal("No gym leader is waiting for you right now."));
    }
}
