package org.ppvon.ultimateCobblemonProgression.common.regions;

import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.Collection;

/**
 * Regional forms (Alolan, Galarian, Hisuian, Paldean, and Pokémon biased to evolve into one) belong to the region a
 * region.json claims them for with "formAspects". With lockRegionalForms on, such a Pokémon only spawns for players
 * who have that region open, on top of its species' own tier and region. Its tier and levels stay its species'.
 *
 * <p>Works on Cobblemon aspects: both a spawn entry's properties ("rattata alolan", "pikachu region_bias=alola") and
 * a spawned Pokémon carry them ("alolan", "region-bias-alola").
 */
public final class RegionalForms {
    private RegionalForms() {}

    /** Whether a Pokémon with these aspects may spawn for the player, as far as its regional form goes. */
    public static boolean isAllowed(Collection<String> aspects, ServerPlayer player) {
        if (!ConfigLoader.LOCK_REGIONAL_FORMS.get() || !RegionRegistry.isActive()) return true;
        int region = RegionRegistry.formRegion(aspects);
        if (region < 0) return true;
        return player != null && RegionProgressTracker.get(player).step(region) > 0;
    }
}
