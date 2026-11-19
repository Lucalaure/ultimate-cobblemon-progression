package org.ppvon.ultimateCobblemonProgression.common.regions;

import com.cobblemon.mod.common.api.pokedex.AbstractPokedexManager;
import com.cobblemon.mod.common.api.pokedex.PokedexEntryProgress;
import com.cobblemon.mod.common.api.pokemon.evolution.Evolution;
import com.cobblemon.mod.common.api.spawning.CobblemonSpawnPools;
import com.cobblemon.mod.common.api.spawning.detail.PokemonHerdSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.PokemonSpawnDetail;
import com.cobblemon.mod.common.api.spawning.detail.SpawnDetail;
import com.cobblemon.mod.common.pokemon.FormData;
import com.cobblemon.mod.common.pokemon.Species;
import com.cobblemon.mod.common.util.PlayerExtensionsKt;
import net.minecraft.server.level.ServerPlayer;
import org.ppvon.ultimateCobblemonProgression.UltimateCobblemonProgression;
import org.ppvon.ultimateCobblemonProgression.common.influence.TrainerLevelInfluence;
import org.ppvon.ultimateCobblemonProgression.config.ConfigLoader;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The {@code catchWholeTier} difficulty option: in every region, reaching tier N+1 also needs every obtainable
 * Pokémon of tier N caught. In a league this holds the next gym leader back (see GymLeaderPicker); the extra starting
 * region, which has no leaders, simply waits at its tier until it's caught, without ever holding the league back. "Obtainable" means it can spawn in the wild (Cobblemon's world spawn pool, so datapacks
 * that add spawns count) or evolves from something that can; anything else, like most legendaries, is skipped so
 * no one gets stuck. Catching, evolving and trading all register a Pokémon as caught in the Pokédex.
 */
public final class TierCatches {
    /** Species that can be obtained, or null until the spawn pools are loaded. */
    private static Set<Species> obtainable;

    private TierCatches() {}

    /** Whether the option applies to this region: it's on (it covers every region). */
    public static boolean applies(RegionDef region) {
        return ConfigLoader.CATCH_WHOLE_TIER.get();
    }

    /** Forgets the obtainable species, e.g. after a datapack reload changed the spawn pools. */
    public static void invalidate() {
        obtainable = null;
    }

    /** The species of this tier the player still has to catch, in Pokédex order. Empty when the option is off. */
    public static List<Species> missing(ServerPlayer player, RegionDef region, int tier) {
        if (!applies(region) || tier < 1) return List.of();
        AbstractPokedexManager dex = PlayerExtensionsKt.pokedex(player);
        Set<Species> canGet = obtainable();
        List<Species> out = new ArrayList<>();
        for (Species sp : region.species(tier)) {
            if (!sp.getImplemented() || !canGet.contains(sp)) continue;
            if (dex == null || dex.getKnowledgeForSpecies(sp.getResourceIdentifier()) != PokedexEntryProgress.OWNED) {
                out.add(sp);
            }
        }
        out.sort(Comparator.comparingInt(Species::getNationalPokedexNumber));
        return out;
    }

    /** How many of this tier's species count for the option: the obtainable ones. */
    public static int targets(RegionDef region, int tier) {
        Set<Species> canGet = obtainable();
        return (int) region.species(tier).stream().filter(sp -> sp.getImplemented() && canGet.contains(sp)).count();
    }

    /** "Pidgey, Rattata and 3 more". */
    public static String names(List<Species> species, int shown) {
        List<String> names = species.stream().limit(shown).map(sp -> sp.getTranslatedName().getString()).toList();
        String list = String.join(", ", names);
        int rest = species.size() - names.size();
        return rest > 0 ? list + " and " + rest + " more" : list;
    }

    private static Set<Species> obtainable() {
        if (obtainable == null) {
            Set<Species> found = compute();
            if (found.isEmpty()) return found; // the spawn pools aren't loaded yet; try again next time
            obtainable = found;
            UltimateCobblemonProgression.LOG.info("{} species can be obtained in the wild or by evolution", found.size());
        }
        return obtainable;
    }

    private static Set<Species> compute() {
        Set<Species> found = new HashSet<>();
        try {
            for (SpawnDetail detail : CobblemonSpawnPools.WORLD_SPAWN_POOL.getDetails()) {
                if (detail instanceof PokemonSpawnDetail p) {
                    add(found, p.getPokemon().getSpecies());
                } else if (detail instanceof PokemonHerdSpawnDetail herd) {
                    for (var member : herd.getHerdablePokemon()) add(found, member.getPokemon().getSpecies());
                }
            }
        } catch (RuntimeException e) { // Cobblemon's spawn pool isn't initialised yet
            return Set.of();
        }

        // Anything a wild species evolves into can be obtained too.
        Deque<Species> queue = new ArrayDeque<>(found);
        while (!queue.isEmpty()) {
            Species sp = queue.poll();
            List<Evolution> evolutions = new ArrayList<>(sp.getEvolutions());
            for (FormData form : sp.getForms()) evolutions.addAll(form.getEvolutions());
            for (Evolution evolution : evolutions) {
                Species result = TrainerLevelInfluence.speciesOf(evolution.getResult().getSpecies());
                if (result != null && found.add(result)) queue.add(result);
            }
        }
        return found;
    }

    private static void add(Set<Species> found, String speciesStr) {
        Species sp = TrainerLevelInfluence.speciesOf(speciesStr);
        if (sp != null) found.add(sp);
    }
}
