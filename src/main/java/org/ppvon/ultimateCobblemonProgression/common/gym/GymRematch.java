package org.ppvon.ultimateCobblemonProgression.common.gym;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionDef;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgress;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgressTracker;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierDef;

import java.util.ArrayList;
import java.util.List;

/**
 * Rematches: sneaking and using a Gym Challenge block with an empty hand opens a menu of every league trainer the
 * player has already beaten, on Normal or Hard. The leader is summoned like any other, but tagged
 * {@value #REMATCH_TAG}: Radical Cobblemon Trainers then lets the battle start whatever the player's series, trainer
 * order or level cap (see the RCT rematch mixin), since a rematch changes no progress.
 */
public final class GymRematch {
    /** Put on leaders summoned for a rematch. */
    public static final String REMATCH_TAG = "ucp_gym_rematch";
    /** How far from the gym a player may be when they pick from the menu. */
    private static final double MAX_PICK_DISTANCE = 8;

    private GymRematch() {}

    public static void register() {
        PayloadTypeRegistry.playS2C().register(RematchPayloads.Open.TYPE, RematchPayloads.Open.CODEC);
        PayloadTypeRegistry.playC2S().register(RematchPayloads.Pick.TYPE, RematchPayloads.Pick.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(RematchPayloads.Pick.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            if (player.distanceToSqr(payload.gym().getCenter()) > MAX_PICK_DISTANCE * MAX_PICK_DISTANCE) return;
            if (player.serverLevel().getBlockEntity(payload.gym()) instanceof GymChallengeBlockEntity gym) {
                gym.rematch(player, payload.trainerId());
            }
        });
    }

    /**
     * Every league trainer slot the player has beaten, journey regions first: slots of tiers they've unlocked (or of a
     * completed region), and any slot whose trainer RCT says they've beaten.
     */
    public static List<RematchPayloads.Entry> rematchable(ServerPlayer player) {
        List<RematchPayloads.Entry> entries = new ArrayList<>();
        if (!RegionRegistry.isActive() || !RctBridge.isLoaded()) return entries;
        RegionProgress progress = RegionProgressTracker.get(player);
        for (int r : progress.priorityOrder()) {
            if (!progress.isUnlocked(r)) continue;
            RegionDef region = RegionRegistry.get(r);
            boolean completed = progress.isCompleted(r);
            int step = progress.step(r);
            for (TierDef tier : region.tiers.values()) {
                for (List<String> group : tier.trainers) {
                    if (!completed && tier.index > step && !beatenAny(player, group)) continue;
                    String hard = GymDifficulty.hardVersion(group);
                    entries.add(new RematchPayloads.Entry(region.title, RctBridge.trainerName(group.get(0)),
                            group.get(0), hard != null ? hard : ""));
                }
            }
        }
        return entries;
    }

    /** Whether the trainer is one the player may rematch: a version of a slot in {@link #rematchable}. */
    static boolean canRematch(ServerPlayer player, String trainerId) {
        for (RematchPayloads.Entry e : rematchable(player)) {
            if (e.normalId().equals(trainerId) || (!e.hardId().isEmpty() && e.hardId().equals(trainerId))) return true;
        }
        return false;
    }

    /** The region a league trainer belongs to (the first listing it), for the gym's chat line. */
    static RegionDef regionOf(String trainerId) {
        for (RegionDef region : RegionRegistry.ordered()) {
            for (TierDef tier : region.tiers.values()) {
                for (List<String> group : tier.trainers) {
                    if (group.contains(trainerId)) return region;
                }
            }
        }
        return null;
    }

    static Component nothingToRematch() {
        return Component.literal("You haven't beaten any gym leaders yet, so there's no one to rematch.")
                .withStyle(ChatFormatting.GRAY);
    }

    private static boolean beatenAny(ServerPlayer player, List<String> group) {
        for (String id : group) {
            if (RctBridge.defeats(player, id) > 0) return true;
        }
        return false;
    }
}
