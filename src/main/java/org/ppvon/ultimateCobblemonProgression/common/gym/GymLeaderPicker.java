package org.ppvon.ultimateCobblemonProgression.common.gym;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionDef;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgress;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgressTracker;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.regions.TierCatches;
import org.ppvon.ultimateCobblemonProgression.common.regions.TierEntry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Works out which trainers a Gym Challenge block can summon for a player: in each open, unfinished region that lists
 * trainers, the first one they haven't beaten yet in its next locked tier. With two leagues in progress (a league as
 * the player's second region) both can have a leader ready, and the player picks one (see {@link GymChoice}).
 */
public final class GymLeaderPicker {
    private GymLeaderPicker() {}

    /** A trainer a gym can summon: which one, for which region. */
    public record Pick(String trainerId, RegionDef region) {}

    /**
     * Every trainer the gym can summon for the player, journey first, and why each other region with trainers has
     * none ready (e.g. Pokémon still to catch). When nothing is ready, {@link #problem} says why.
     */
    public record Result(List<Pick> ready, List<Component> waiting, Component problem) {
        static Result none(Component why) {
            return new Result(List.of(), List.of(), why);
        }

        public boolean found() {
            return !ready.isEmpty();
        }
    }

    public static Result pickAll(ServerPlayer player) {
        if (!RegionRegistry.isActive()) {
            return Result.none(Component.literal("Gyms only work when region data is loaded."));
        }
        if (!RctBridge.isLoaded()) {
            return Result.none(Component.literal("Gyms need Radical Cobblemon Trainers to be installed."));
        }
        if (RegionProgress.needsHomeRegion(player)) {
            return Result.none(Component.literal("Choose where your journey starts first: open your Trainer Card."));
        }

        RegionProgress progress = RegionProgressTracker.get(player);
        List<RegionDef> regions = RegionRegistry.ordered();
        List<Pick> ready = new ArrayList<>();
        List<Component> waiting = new ArrayList<>();

        for (int r : progress.priorityOrder()) {
            if (!progress.isUnlocked(r) || progress.isCompleted(r)) continue;
            RegionDef region = regions.get(r);
            TierDef next = region.tiers.get(progress.step(r) + 1);
            if (next == null || next.trainers.isEmpty()) continue;

            // catchWholeTier: the region's next leader waits until its current tier is fully caught.
            List<TierEntry> missing = TierCatches.missing(player, region, progress.step(r));
            if (!missing.isEmpty()) {
                waiting.add(Component.literal("Catch every tier " + progress.step(r) + " Pokémon of " + region.title
                        + " first. Still to catch (" + missing.size() + "): " + TierCatches.names(missing, 5) + "."));
                continue;
            }

            boolean found = false;
            for (List<String> group : next.trainers) {
                boolean beaten = false;
                for (String trainerId : group) {
                    int defeats = RctBridge.defeats(player, trainerId);
                    if (defeats < 0) {
                        return Result.none(Component.literal("Couldn't look up the trainer '" + trainerId
                                + "' in Radical Cobblemon Trainers. Check the region data and the server log."));
                    }
                    if (defeats > 0) {
                        beaten = true;
                        break;
                    }
                }
                if (!beaten) {
                    ready.add(new Pick(GymDifficulty.trainerFor(player, group), region));
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Every listed trainer is beaten but the tier is still locked, so its other requirements aren't met.
                waiting.add(Component.literal("You've beaten every " + region.title
                        + " trainer for your next step. Meet its other requirements to move on."));
            }
        }

        if (!ready.isEmpty()) return new Result(ready, waiting, null);
        if (!waiting.isEmpty()) {
            Component problem = waiting.size() == 1 ? waiting.get(0) : join(waiting);
            return new Result(List.of(), waiting, problem);
        }
        if (RegionProgress.needsNextRegion(player)) {
            return Result.none(Component.literal("You've completed your region! Choose where your journey goes next: open your Trainer Card."));
        }
        return Result.none(Component.literal("No gym leader is waiting for you right now."));
    }

    private static Component join(List<Component> lines) {
        var out = Component.empty();
        for (int i = 0; i < lines.size(); i++) {
            if (i > 0) out.append("\n");
            out.append(lines.get(i));
        }
        return out;
    }
}
