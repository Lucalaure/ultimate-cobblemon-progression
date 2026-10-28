package org.ppvon.ultimateCobblemonProgression.common.progression;

import com.cobblemon.mod.common.pokemon.Species;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.api.UCPApi;
import org.ppvon.ultimateCobblemonProgression.common.component.TrainerLevelComponents;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionDef;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionProgressTracker;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionRegistry;
import org.ppvon.ultimateCobblemonProgression.common.regions.RegionSpecies;
import org.ppvon.ultimateCobblemonProgression.common.tiers.TierRegistry;

/**
 * Where a species sits relative to a player, in whichever progression mode is active:
 * the global tier ladder (tier files + trainer level), or region mode (one ladder per region).
 *
 * @param allowed    whether the species may spawn for this player
 * @param speciesCap level cap of the species' own tier
 * @param playerCap  level cap of the player's tier on the same ladder
 * @param tierGap    how many tiers the player is past the species (negative if the species is ahead)
 */
public record TierStanding(boolean allowed, int speciesCap, int playerCap, int tierGap) {
    private static final int DEFAULT_CAP = 100;

    /** Whether any tier data is loaded at all; without it, UCP leaves spawning alone. */
    public static boolean hasTierData() {
        return RegionRegistry.isActive() || TierRegistry.maxTier() > 0;
    }

    /** Whether the species has a tier (in region mode: belongs to a region). */
    public static boolean isKnown(Species species) {
        return RegionRegistry.isActive()
                ? RegionSpecies.region(species) >= 0
                : UCPApi.getTier(species) > 0;
    }

    /** Only valid for species where {@link #isKnown} is true. */
    public static TierStanding of(ServerPlayer player, Species species) {
        return RegionRegistry.isActive() ? ofRegion(player, species) : ofTiers(player, species);
    }

    private static TierStanding ofTiers(ServerPlayer player, Species species) {
        int speciesTier = UCPApi.getTier(species);
        int playerTier = Math.max(0, TrainerLevelComponents.KEY.get(player).getLevel());
        return new TierStanding(
                speciesTier <= playerTier,
                tierCap(speciesTier),
                tierCap(playerTier),
                playerTier - speciesTier);
    }

    private static TierStanding ofRegion(ServerPlayer player, Species species) {
        int region = RegionSpecies.region(species);
        int speciesTier = RegionSpecies.regionTier(species);
        RegionDef def = RegionRegistry.get(region);
        int step = RegionProgressTracker.get(player).step(region);

        int speciesCap = def.levelCap(speciesTier);
        int playerCap = step > 0 ? def.levelCap(step) : speciesCap;
        return new TierStanding(step > 0 && speciesTier <= step, speciesCap, playerCap, step - speciesTier);
    }

    private static int tierCap(int tier) {
        return TierRegistry.get(tier).map(t -> t.levelCap).orElse(DEFAULT_CAP);
    }
}
