package org.ppvon.ultimateCobblemonProgression.common.progression;

import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.api.UCPApi;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionDef;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgress;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgressTracker;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionSpecies;
import org.ppvon.ultimateCobblemonProgression.common.regions.TierEntry;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierRegistry;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.Collection;
import java.util.List;

/**
 * Where a species sits relative to a player, in whichever progression mode is active:
 * the global tier ladder (tier files + trainer level), or region mode (one ladder per region).
 *
 * <p>In region mode, while the player is working through a region, a species can spawn from up to three
 * <b>spawn pools</b>, each with its own levels and weight:
 * <ul>
 *   <li>{@link Pool.Kind#REGION}: species of a region still in progress (the current one, or the extra starting
 *       region), measured against the player's progress in that region. The heaviest pool.</li>
 *   <li>{@link Pool.Kind#PAST_IN_CAP}: species of completed regions whose tier is at or below the player's tier in
 *       the current region, at the current region's levels, so they can join the player's new team.</li>
 *   <li>{@link Pool.Kind#PAST_FULL}: every species of completed regions, at their own fully unlocked levels.
 *       The rarest pool.</li>
 * </ul>
 * A species of a completed region within the cap is in both past pools; its weight is the sum of the two, and
 * each spawn's level comes from one of them, picked in proportion to their weights. When no region is in
 * progress (or pools are off), every species has a single pool measured against its own region, as before.
 *
 * @param allowed whether the species may spawn for this player
 * @param pools   the pools the species spawns from, never empty
 */
public record TierStanding(boolean allowed, List<Pool> pools) {
    private static final int DEFAULT_CAP = 100;

    /**
     * One pool a species spawns from.
     *
     * @param speciesCap level cap the species' level is based on
     * @param playerCap  level cap of the player on the same ladder, which boosts lower-tier species
     * @param tierGap    how many tiers the player is past the species (negative if the species is ahead)
     * @param weight     the pool's spawn weight multiplier
     */
    public record Pool(Kind kind, int speciesCap, int playerCap, int tierGap, double weight) {
        public enum Kind { REGION, PAST_IN_CAP, PAST_FULL }
    }

    /** The species' first pool: its own region (or tier ladder), or a completed region at its full levels. */
    public Pool primary() {
        return pools.get(0);
    }

    /** Whether any tier data is loaded at all; without it, UCP leaves spawning alone. */
    public static boolean hasTierData() {
        return RegionRegistry.isActive() || TierRegistry.maxTier() > 0;
    }

    /** Whether the species has a tier (in region mode: belongs to a region). */
    public static boolean isKnown(Species species) {
        return isKnown(species, List.of());
    }

    /**
     * Whether a Pokémon of this species with these aspects has a tier. In region mode its regional form (e.g.
     * "alolan") can belong to a region even when its species doesn't.
     */
    public static boolean isKnown(Species species, Collection<String> aspects) {
        return RegionRegistry.isActive()
                ? RegionSpecies.entry(species, aspects) != null
                : UCPApi.getTier(species) > 0;
    }

    /** Only valid for species where {@link #isKnown} is true. */
    public static TierStanding of(ServerPlayer player, Species species) {
        return of(player, species, List.of());
    }

    /**
     * Where a Pokémon of this species with these aspects stands: in region mode, a regional form is measured against
     * its own region and tier (see {@link RegionSpecies#entry}). Only valid when {@link #isKnown} is true for them.
     */
    public static TierStanding of(ServerPlayer player, Species species, Collection<String> aspects) {
        if (!RegionRegistry.isActive()) return ofTiers(player, species);
        TierEntry entry = RegionSpecies.entry(species, aspects);
        return ofRegion(player, entry.region(), entry.tier());
    }

    private static TierStanding ofTiers(ServerPlayer player, Species species) {
        int speciesTier = UCPApi.getTier(species);
        int playerTier = Math.max(0, TrainerLevelComponents.KEY.get(player).getLevel());
        return single(speciesTier <= playerTier, tierCap(speciesTier), tierCap(playerTier), playerTier - speciesTier);
    }

    private static TierStanding ofRegion(ServerPlayer player, int region, int speciesTier) {
        RegionDef def = RegionRegistry.get(region);
        RegionProgress progress = RegionProgressTracker.get(player);
        int step = progress.step(region);

        int speciesCap = def.levelCap(speciesTier);
        int playerCap = step > 0 ? def.levelCap(step) : speciesCap;
        boolean allowed = step > 0 && speciesTier <= step;
        int current = progress.current();

        if (!ConfigLoader.DO_REGION_POOLS.get() || current < 0) {
            return single(allowed, speciesCap, playerCap, step - speciesTier);
        }
        if (!progress.isCompleted(region)) {
            // A region in progress: the current one, or the extra starting region.
            return new TierStanding(allowed, List.of(new Pool(Pool.Kind.REGION, speciesCap, playerCap,
                    step - speciesTier, ConfigLoader.REGION_POOL_WEIGHT.get())));
        }

        // A completed region: always at its full levels, and also at the current region's levels when the
        // species' tier is within the player's tier there.
        Pool full = new Pool(Pool.Kind.PAST_FULL, speciesCap, playerCap, step - speciesTier,
                ConfigLoader.PAST_FULL_POOL_WEIGHT.get());
        RegionDef currentDef = RegionRegistry.get(current);
        int currentStep = progress.step(current);
        if (currentStep < 1 || speciesTier > currentStep) {
            return new TierStanding(allowed, List.of(full));
        }
        Pool inCap = new Pool(Pool.Kind.PAST_IN_CAP, currentDef.levelCap(speciesTier), currentDef.levelCap(currentStep),
                currentStep - speciesTier, ConfigLoader.PAST_IN_CAP_POOL_WEIGHT.get());
        return new TierStanding(allowed, List.of(full, inCap));
    }

    private static TierStanding single(boolean allowed, int speciesCap, int playerCap, int tierGap) {
        return new TierStanding(allowed, List.of(new Pool(Pool.Kind.REGION, speciesCap, playerCap, tierGap, 1.0)));
    }

    private static int tierCap(int tier) {
        return TierRegistry.get(tier).map(t -> t.levelCap).orElse(DEFAULT_CAP);
    }
}
