package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.cobblemon.mod.common.api.pokemon.PokemonProperties;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Regional forms in region mode. A regional form (Alolan, Galarian, Hisuian, Paldean) belongs to the region whose
 * region.json claims its aspect in "formAspects", at its own tier (see {@link RegionSpecies}): it spawns, levels and
 * counts for catchWholeTier there, whether or not its species' region is open.
 *
 * <p>Pokémon that always evolve into a regional form (a "region-bias-alola" Pikachu) never spawn in region mode:
 * every regional form they lead to spawns in the wild itself, and they'd otherwise let a player raise a regional form
 * without its region. Evolving a normal Pikachu in an Alola-style biome still gives an Alolan Raichu.
 *
 * <p>Works on Cobblemon aspects: a spawned Pokémon carries them ("alolan", "region-bias-alola"), and so do a spawn
 * entry's properties ("rattata alolan"), except region bias, which spawn entries set as "region_bias=alola".
 */
public final class RegionalForms {
    private static final Pattern REGION_BIAS = Pattern.compile("(?:^|\\s)region_bias=([a-z_]+)");

    private RegionalForms() {}

    /** Whether a Pokémon with these aspects may spawn at all, as far as its regional form goes. */
    public static boolean maySpawn(Collection<String> aspects) {
        return !RegionRegistry.isActive() || !RegionSpecies.isRegionBias(aspects);
    }

    /** A spawn entry's aspects, with its region bias ("region_bias=alola") as the aspect Cobblemon gives it. */
    public static Set<String> aspectsOf(PokemonProperties props) {
        Set<String> aspects = new HashSet<>(props.getAspects());
        String original = props.getOriginalString();
        if (original != null && original.contains("region_bias")) {
            Matcher m = REGION_BIAS.matcher(original.toLowerCase(Locale.ROOT));
            if (m.find()) aspects.add(RegionSpecies.REGION_BIAS_PREFIX + m.group(1));
        }
        return aspects;
    }
}
