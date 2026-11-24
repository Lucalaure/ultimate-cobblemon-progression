package org.ppvon.ultimateCobblemonProgression.common.regions;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.common.gym.LeagueLevelCap;
import org.ppvon.ultimateCobblemonProgression.common.gym.RctBridge;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Caches each online player's {@link RegionProgress} (it's read on every spawn attempt) and announces
 * newly unlocked tiers and regions when it changes.
 */
public final class RegionProgressTracker {
    private static final Map<UUID, RegionProgress> CACHE = new ConcurrentHashMap<>();

    private RegionProgressTracker() {}

    public static RegionProgress get(ServerPlayer player) {
        return CACHE.computeIfAbsent(player.getUUID(), id -> RegionProgress.compute(player));
    }

    /** Recomputes a player's progress and tells them about anything newly unlocked. */
    public static void refresh(ServerPlayer player) {
        if (!RegionRegistry.isActive()) return;
        RegionProgress now = RegionProgress.compute(player);
        RegionProgress before = CACHE.put(player.getUUID(), now);
        LeagueLevelCap.update(player);
        if (before != null && before.size() == now.size()) {
            announce(player, before, now);
        }
    }

    /** Recomputes without messages, e.g. on join or after a datapack reload changed the regions. */
    public static void refreshSilently(ServerPlayer player) {
        if (!RegionRegistry.isActive()) {
            CACHE.remove(player.getUUID());
            return;
        }
        CACHE.put(player.getUUID(), RegionProgress.compute(player));
        LeagueLevelCap.update(player);
    }

    public static void refreshAllSilently(MinecraftServer server) {
        CACHE.clear();
        server.getPlayerList().getPlayers().forEach(RegionProgressTracker::refreshSilently);
    }

    public static void forget(ServerPlayer player) {
        CACHE.remove(player.getUUID());
    }

    private static void announce(ServerPlayer player, RegionProgress before, RegionProgress now) {
        List<RegionDef> regions = RegionRegistry.ordered();
        List<String> opened = new ArrayList<>();
        boolean journeyRegionCompleted = false;
        for (int r = 0; r < regions.size(); r++) {
            RegionDef region = regions.get(r);

            if (now.step(r) > before.step(r)) {
                int unlocked = 0;
                for (int t = before.step(r) + 1; t <= now.step(r); t++) unlocked += region.realSpecies(t);
                player.displayClientMessage(regionTag(region)
                        .append(Component.literal(unlocked + " new species can now appear in the wild")
                                .withStyle(ChatFormatting.GREEN)), false);
            }

            if (now.isTierCaught(r) && !before.isTierCaught(r) && now.step(r) == before.step(r)
                    && now.step(r) < region.maxTier()) {
                String next = region.isLeague()
                        ? "Your next gym leader is ready for you."
                        : "Its next tier opens with your next badge.";
                player.displayClientMessage(regionTag(region).append(Component.literal(
                        "You've caught every tier " + now.step(r) + " Pokémon! " + next)
                        .withStyle(ChatFormatting.GREEN)), false);
            }

            if (now.isCompleted(r) && !before.isCompleted(r)) {
                creditRctSeries(player, region);
                player.displayClientMessage(regionTag(region)
                        .append(Component.literal("Region complete!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD)), false);
                if (now.isPlayerPick(r) && region.isLeague()) journeyRegionCompleted = true;
            }

            // Journey regions are announced when they're picked; this lists the ones that opened on their own.
            if (now.isUnlocked(r) && !before.isUnlocked(r) && !now.isOnJourney(r)) opened.add(region.title);
        }

        if (!opened.isEmpty()) {
            player.displayClientMessage(Component.literal("Now open: " + String.join(", ", opened) + ".")
                    .withStyle(ChatFormatting.GOLD), false);
        }
        if (journeyRegionCompleted) {
            if (RegionProgress.needsNextRegion(player)) {
                player.displayClientMessage(Component.literal(
                        "Where does your journey go next? Open your Trainer Card to choose your next region.")
                        .withStyle(ChatFormatting.YELLOW), false);
            } else if (now.activeLeagues().isEmpty()) {
                player.displayClientMessage(Component.literal(
                        "You've completed every league. Congratulations, Champion!")
                        .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD), false);
            } else {
                // Two leagues are a pair: the next region comes once the other one is done too.
                RegionDef other = RegionRegistry.get(now.current());
                player.displayClientMessage(Component.literal("Keep going in " + other.title
                        + ": you'll choose where your journey goes next once it's complete too. Until then, your level cap follows "
                        + other.title + ".").withStyle(ChatFormatting.YELLOW), false);
            }
        }
    }

    /**
     * A league completed outside the player's RCT series (the second of two leagues) isn't one RCT saw them complete,
     * so CJP records it in RCT, which counts completed series towards its loot luck bonus.
     */
    private static void creditRctSeries(ServerPlayer player, RegionDef region) {
        if (region.series.isEmpty() || !RctBridge.isLoaded()) return;
        String current = RctBridge.currentSeries(player);
        if (current != null && !current.equals(region.series.get())) RctBridge.addSeriesCompletion(player, region.series.get());
    }

    private static MutableComponent regionTag(RegionDef region) {
        return Component.literal("[" + region.title + "] ").withStyle(ChatFormatting.AQUA);
    }
}
