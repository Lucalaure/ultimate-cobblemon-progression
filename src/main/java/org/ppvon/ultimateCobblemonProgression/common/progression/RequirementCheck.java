package org.ppvon.ultimateCobblemonProgression.common.progression;

import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.common.progression.dex.DexProgressionApi;
import org.ppvon.ultimateCobblemonProgression.common.tiers.requirements.DexRequirements;
import org.ppvon.ultimateCobblemonProgression.common.tiers.requirements.TierRequirements;

import java.util.HashSet;
import java.util.Set;

/**
 * Checks tier requirements (Pokédex counts and advancements) against a player.
 */
public final class RequirementCheck {
    private static final Set<ResourceLocation> WARNED_MISSING = new HashSet<>();

    private RequirementCheck() {}

    /** Lazily fetches Pokédex counts so requirement checks without a dex part never touch the Pokédex. */
    public static final class DexLookup {
        private final ServerPlayer player;
        private DexProgressionApi.DexCounts counts;

        public DexLookup(ServerPlayer player) {
            this.player = player;
        }

        DexProgressionApi.DexCounts get() {
            if (counts == null) counts = DexProgressionApi.get(player);
            return counts;
        }
    }

    public static boolean isMet(ServerPlayer player, TierRequirements req, DexLookup dex) {
        return isDexMet(req.dex, dex) && areAdvancementsDone(player, req);
    }

    public static boolean isDexMet(DexRequirements req, DexLookup dex) {
        if (!req.hasAny()) return true;
        var counts = dex.get();
        return counts.seen() >= req.seen && counts.caught() >= req.caught;
    }

    public static boolean areAdvancementsDone(ServerPlayer player, TierRequirements req) {
        for (ResourceLocation id : req.advancements) {
            if (!isAdvancementDone(player, id)) return false;
        }
        return true;
    }

    public static boolean isAdvancementDone(ServerPlayer player, ResourceLocation id) {
        AdvancementHolder holder = player.server.getAdvancements().get(id);
        if (holder == null) {
            if (WARNED_MISSING.add(id)) {
                UltimateCobblemonProgression.LOG.warn("Tier requirement references unknown advancement {}", id);
            }
            return false;
        }
        return player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /** The advancement's display title, or its id if it has none. */
    public static String advancementTitle(ServerPlayer player, ResourceLocation id) {
        AdvancementHolder holder = player.server.getAdvancements().get(id);
        if (holder != null && holder.value().display().isPresent()) {
            return holder.value().display().get().getTitle().getString();
        }
        return id.toString();
    }
}
