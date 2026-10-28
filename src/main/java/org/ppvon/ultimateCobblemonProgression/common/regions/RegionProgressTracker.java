package org.ppvon.ultimateCobblemonProgression.common.regions;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

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
        for (int r = 0; r < regions.size(); r++) {
            RegionDef region = regions.get(r);

            if (now.step(r) > before.step(r)) {
                int unlocked = 0;
                for (int t = before.step(r) + 1; t <= now.step(r); t++) unlocked += region.realSpecies(t);
                player.displayClientMessage(regionTag(region)
                        .append(Component.literal(unlocked + " new species can now appear in the wild")
                                .withStyle(ChatFormatting.GREEN)), false);
            }

            if (now.isCompleted(r) && !before.isCompleted(r)) {
                MutableComponent msg = regionTag(region)
                        .append(Component.literal("Region complete!").withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD));
                RegionDef next = RegionRegistry.get(r + 1);
                if (next != null) {
                    msg.append(Component.literal(" " + next.title + " is now open.").withStyle(ChatFormatting.GOLD));
                }
                player.displayClientMessage(msg, false);
            }
        }
    }

    private static MutableComponent regionTag(RegionDef region) {
        return Component.literal("[" + region.title + "] ").withStyle(ChatFormatting.AQUA);
    }
}
