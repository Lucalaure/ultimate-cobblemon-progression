package org.ppvon.ultimateCobblemonProgression.common.tiers;

import net.minecraft.resources.ResourceLocation;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ppvon.ultimateCobblemonProgression.common.tiers.requirements.TierRequirements;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TierRegistry {
    private static final Logger LOG = LogManager.getLogger("CJP/Tiers");
    private static final Pattern TIER_NAME = Pattern.compile("^tier_(\\d+)$");

    private static final Map<Integer, TierDef> BY_TIER = new HashMap<>();
    private static final Map<ResourceLocation, Integer> SPECIES_MIN_TIER = new HashMap<>();
    private static final Map<Integer, Integer> REAL_SEPCIES_BY_TIER = new HashMap<>();
    private static int maxTier = 0;

    private TierRegistry() {}

    public static void clear() {
        BY_TIER.clear();
        SPECIES_MIN_TIER.clear();
        maxTier = 0;
    }

    public static int getRealSpecies(int tier) {
        return REAL_SEPCIES_BY_TIER.get(tier);
    }

    public static void putRealSpecies(int tierIndex, int count) {
        REAL_SEPCIES_BY_TIER.put(tierIndex, count);
    }

    public static boolean put(TierDef def) {
        TierDef prev = BY_TIER.put(def.index, def);
        maxTier = Math.max(maxTier, def.index);

        if (prev != null) {
            rebuildSpeciesMinTier();
            return true;
        } else {
            for (var s : def.species) {
                SPECIES_MIN_TIER.merge(s, def.index, Math::min);
            }
            return false;
        }
    }

    private static void rebuildSpeciesMinTier() {
        SPECIES_MIN_TIER.clear();
        for (var tier : BY_TIER.values()) {
            for (var s : tier.species) {
                SPECIES_MIN_TIER.merge(s, tier.index, Math::min);
            }
        }
    }

    public static TierRequirements getRequirements(int tier) {
        if(tier > maxTier()) {
            return null;
        }
        return BY_TIER.get(tier).requirements;
    }

    public static int maxTier() {
        return maxTier;
    }

    public static Optional<TierDef> get(int idx) {
        return Optional.ofNullable(BY_TIER.get(idx));
    }

    public static int getMinTierFor(ResourceLocation speciesId) {
        return SPECIES_MIN_TIER.getOrDefault(speciesId, Integer.MAX_VALUE);
    }

    public static int getCapForTier(int tier) {
        return BY_TIER.get(tier).levelCap;
    }

    public static OptionalInt tryParseTierIndex(String pathNoFolders) {
        Matcher m = TIER_NAME.matcher(pathNoFolders);
        if (!m.matches()) return OptionalInt.empty();
        return OptionalInt.of(Integer.parseInt(m.group(1)));
    }

    public static String summary() {
        return "tiers=" + BY_TIER.size() + " max=" + maxTier + " species=" + SPECIES_MIN_TIER.size();
    }

    public static Map<Integer, TierDef> exportSpeciesByTier() {
        /*
        Map<Integer, Set<ResourceLocation>> out = new HashMap<>();
        for (TierDef def : BY_TIER.values()) {
            if (def == null) continue;
            Set<ResourceLocation> species = (def.species != null) ? Set.copyOf(def.species) : Set.of();
            out.put(def.index, species);
        }
        return out;

         */
        return BY_TIER;
    }


}
